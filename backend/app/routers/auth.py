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
        expires_at=datetime.now(timezone.utc) + timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS),
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
    if token_row.expires_at < datetime.now(timezone.utc):
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
