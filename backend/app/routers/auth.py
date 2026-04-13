from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
import hashlib
from datetime import datetime, timedelta, timezone

from app.database import get_db
from app.models.refresh_token import RefreshToken
from app.schemas.auth import (
    RegisterRequest,
    LoginRequest,
    GoogleAuthRequest,
    TokenResponse,
    RefreshTokenRequest,
)
from app.services.auth_service import register_user, authenticate_user, create_tokens
from app.utils.jwt import verify_token, create_access_token, create_refresh_token
from app.utils.timezone import now_utc, ensure_aware_utc
from app.config import get_settings

settings = get_settings()
router = APIRouter()


def _hash_token(token: str) -> str:
    """SHA-256 hash a token for safe DB storage."""
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def _store_refresh_token(db: Session, user_id: str, raw_token: str) -> None:
    """Store a hashed refresh token in the DB."""
    db.add(RefreshToken(
        user_id=user_id,
        token_hash=_hash_token(raw_token),
        expires_at=now_utc() + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS),
    ))
    db.commit()


def _revoke_refresh_token(db: Session, raw_token: str) -> bool:
    """Revoke a refresh token by its hash. Returns True if found."""
    token_row = db.query(RefreshToken).filter(
        RefreshToken.token_hash == _hash_token(raw_token),
        RefreshToken.revoked == False,
    ).first()
    if token_row:
        token_row.revoked = True
        db.commit()
        return True
    return False


@router.post("/register", response_model=TokenResponse)
def register(data: RegisterRequest, db: Session = Depends(get_db)):
    """Register a new user with email/password."""
    try:
        user = register_user(db, data)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    tokens = create_tokens(user)
    _store_refresh_token(db, user.id, tokens.refresh_token)
    return tokens


@router.post("/login", response_model=TokenResponse)
def login(data: LoginRequest, db: Session = Depends(get_db)):
    """Login with email/password."""
    try:
        user = authenticate_user(db, data.email, data.password)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=str(e))
    tokens = create_tokens(user)
    _store_refresh_token(db, user.id, tokens.refresh_token)
    return tokens


@router.post("/google", response_model=TokenResponse)
def google_auth(data: GoogleAuthRequest, db: Session = Depends(get_db)):
    """Login or register via Google OAuth (Firebase ID token)."""
    from app.utils.firebase import verify_google_id_token
    from app.models.user import User

    try:
        google_user = verify_google_id_token(data.id_token)
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid Google token",
        )

    user = db.query(User).filter(User.email == google_user["email"]).first()
    if not user:
        user = User(
            name=google_user["name"],
            email=google_user["email"],
            auth_provider="google",
            role="customer",
        )
        db.add(user)
        db.commit()
        db.refresh(user)

    tokens = create_tokens(user)
    _store_refresh_token(db, user.id, tokens.refresh_token)
    return tokens


@router.post("/refresh", response_model=TokenResponse)
def refresh_token(data: RefreshTokenRequest, db: Session = Depends(get_db)):
    """Refresh access token using a valid refresh token (with rotation).
    
    The old refresh token is revoked and a new one is issued.
    """
    payload = verify_token(data.refresh_token, token_type="refresh")

    # Verify the token exists and is not revoked
    token_hash = _hash_token(data.refresh_token)
    token_row = db.query(RefreshToken).filter(
        RefreshToken.token_hash == token_hash,
        RefreshToken.revoked == False,
    ).first()

    if not token_row:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token has been revoked or is invalid",
        )

    # Check expiry
    token_expiry = ensure_aware_utc(token_row.expires_at)
    if token_expiry and token_expiry < now_utc():
        token_row.revoked = True
        db.commit()
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token has expired",
        )

    # Revoke the old token
    token_row.revoked = True
    db.commit()

    # Issue new tokens
    user_id = payload["sub"]
    role = payload.get("role", "customer")
    new_access = create_access_token({"sub": user_id, "role": role})
    new_refresh = create_refresh_token({"sub": user_id, "role": role})

    # Store the new refresh token
    _store_refresh_token(db, user_id, new_refresh)

    return TokenResponse(
        access_token=new_access,
        refresh_token=new_refresh,
    )


@router.post("/logout")
def logout(data: RefreshTokenRequest, db: Session = Depends(get_db)):
    """Logout — revoke the provided refresh token."""
    revoked = _revoke_refresh_token(db, data.refresh_token)
    if not revoked:
        # Still return success (don't leak whether token existed)
        pass
    return {"message": "Logged out successfully"}


# --- OTP-based Phone Signup ---

# In-memory OTP store (for production, use Redis or a DB table)
_otp_store: dict[str, dict] = {}

@router.post("/otp/send")
def send_otp(data: dict, db: Session = Depends(get_db)):
    """Send OTP to a phone number for registration/login."""
    import random
    phone = data.get("phone", "").strip()
    if not phone or len(phone) != 10 or not phone.isdigit():
        raise HTTPException(status_code=400, detail="Enter a valid 10-digit phone number")
    
    otp = str(random.randint(100000, 999999))
    _otp_store[phone] = {
        "otp": otp,
        "expires": now_utc() + timedelta(minutes=5),
    }
    # In production, send via SMS gateway (e.g., Twilio, MSG91)
    # For now, return it in response for development
    return {"message": f"OTP sent to {phone[:3]}****{phone[-3:]}", "dev_otp": otp}


@router.post("/otp/verify", response_model=TokenResponse)
def verify_otp(data: dict, db: Session = Depends(get_db)):
    """Verify OTP and login/register the user."""
    from app.models.user import User
    
    phone = data.get("phone", "").strip()
    otp = data.get("otp", "").strip()
    name = data.get("name", "").strip()
    
    if not phone or not otp:
        raise HTTPException(status_code=400, detail="Phone and OTP are required")
    
    stored = _otp_store.get(phone)
    if not stored:
        raise HTTPException(status_code=400, detail="No OTP was sent to this number. Please request a new one.")
    
    stored_expiry = ensure_aware_utc(stored.get("expires"))
    if stored_expiry and now_utc() > stored_expiry:
        del _otp_store[phone]
        raise HTTPException(status_code=400, detail="OTP has expired. Please request a new one.")
    
    if stored["otp"] != otp:
        raise HTTPException(status_code=400, detail="Invalid OTP")
    
    # OTP verified — clean up
    del _otp_store[phone]
    
    # Find or create user
    user = db.query(User).filter(User.phone == phone).first()
    if not user:
        if not name:
            raise HTTPException(status_code=400, detail="Name is required for new users")
        user = User(
            name=name,
            phone=phone,
            auth_provider="phone",
            role="customer",
        )
        db.add(user)
        db.commit()
        db.refresh(user)
    
    tokens = create_tokens(user)
    _store_refresh_token(db, user.id, tokens.refresh_token)
    return tokens
