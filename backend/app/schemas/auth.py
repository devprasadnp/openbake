from pydantic import BaseModel, EmailStr
from typing import Optional


# --- Auth Schemas ---

class RegisterRequest(BaseModel):
    name: str
    email: EmailStr
    password: str
    phone: Optional[str] = None


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class GoogleAuthRequest(BaseModel):
    id_token: str


class PhoneSendOtpRequest(BaseModel):
    phone: str


class PhoneVerifyOtpRequest(BaseModel):
    phone: str
    otp: str


class TokenResponse(BaseModel):
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


class RefreshTokenRequest(BaseModel):
    refresh_token: str


# --- User / Profile Schemas ---

class UserResponse(BaseModel):
    id: str
    name: str
    email: Optional[str] = None
    phone: Optional[str] = None
    auth_provider: str
    role: str

    class Config:
        from_attributes = True


class ProfileUpdateRequest(BaseModel):
    name: Optional[str] = None
    phone: Optional[str] = None


# --- Address Schemas ---

class AddressCreate(BaseModel):
    label: str = "Home"
    full_address: str
    city: str
    pincode: str
    lat: Optional[float] = None
    lng: Optional[float] = None
    is_default: bool = False


class AddressResponse(BaseModel):
    id: str
    label: str
    full_address: str
    city: str
    pincode: str
    lat: Optional[float] = None
    lng: Optional[float] = None
    is_default: bool

    class Config:
        from_attributes = True
