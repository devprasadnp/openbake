from sqlalchemy.orm import Session
import bcrypt

from app.models.user import User
from app.schemas.auth import RegisterRequest, TokenResponse
from app.utils.jwt import create_access_token, create_refresh_token


def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def verify_password(plain: str, hashed: str) -> bool:
    return bcrypt.checkpw(plain.encode("utf-8"), hashed.encode("utf-8"))


def register_user(db: Session, data: RegisterRequest) -> User:
    """Register a new user with email/password."""
    existing = db.query(User).filter(User.email == data.email).first()
    if existing:
        raise ValueError("Email already registered")

    if data.phone:
        existing_phone = db.query(User).filter(User.phone == data.phone).first()
        if existing_phone:
            raise ValueError("Phone number already registered")

    user = User(
        name=data.name,
        email=data.email,
        phone=data.phone,
        password_hash=hash_password(data.password),
        auth_provider="email",
        role="customer",
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user


def authenticate_user(db: Session, email: str, password: str) -> User:
    """Authenticate a user by email/password."""
    user = db.query(User).filter(User.email == email).first()
    if not user or not user.password_hash:
        raise ValueError("Invalid email or password")
    if not verify_password(password, user.password_hash):
        raise ValueError("Invalid email or password")
    return user


def create_tokens(user: User) -> TokenResponse:
    """Generate access + refresh tokens for a user."""
    token_data = {"sub": str(user.id), "role": user.role}
    return TokenResponse(
        access_token=create_access_token(token_data),
        refresh_token=create_refresh_token(token_data),
    )
