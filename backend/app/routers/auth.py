from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.schemas.auth import (
    RegisterRequest,
    LoginRequest,
    GoogleAuthRequest,
    TokenResponse,
    RefreshTokenRequest,
)
from app.services.auth_service import register_user, authenticate_user, create_tokens
from app.utils.jwt import verify_token, create_access_token

router = APIRouter()


@router.post("/register", response_model=TokenResponse)
def register(data: RegisterRequest, db: Session = Depends(get_db)):
    """Register a new user with email/password."""
    try:
        user = register_user(db, data)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    return create_tokens(user)


@router.post("/login", response_model=TokenResponse)
def login(data: LoginRequest, db: Session = Depends(get_db)):
    """Login with email/password."""
    try:
        user = authenticate_user(db, data.email, data.password)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=str(e))
    return create_tokens(user)


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

    return create_tokens(user)


@router.post("/refresh", response_model=TokenResponse)
def refresh_token(data: RefreshTokenRequest):
    """Refresh access token using a valid refresh token."""
    payload = verify_token(data.refresh_token, token_type="refresh")
    new_access = create_access_token(
        {"sub": payload["sub"], "role": payload.get("role", "customer")}
    )
    return TokenResponse(
        access_token=new_access,
        refresh_token=data.refresh_token,
    )


@router.post("/logout")
def logout():
    """Logout (client should discard tokens)."""
    return {"message": "Logged out successfully"}
