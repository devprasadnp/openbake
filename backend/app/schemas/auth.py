from pydantic import BaseModel, EmailStr, field_validator
from typing import Optional
import re


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
    profile_image_url: Optional[str] = None

    class Config:
        from_attributes = True


class ProfileUpdateRequest(BaseModel):
    name: Optional[str] = None
    phone: Optional[str] = None
    profile_image_url: Optional[str] = None


# --- Address Schemas ---

class AddressCreate(BaseModel):
    label: str = "Home"
    recipient_name: Optional[str] = None
    recipient_phone: Optional[str] = None
    house_number: Optional[str] = None
    street: Optional[str] = None
    landmark: Optional[str] = None
    full_address: str
    city: str
    state: Optional[str] = None
    pincode: str
    lat: Optional[float] = None
    lng: Optional[float] = None
    is_default: bool = False

    @field_validator("pincode")
    @classmethod
    def validate_pincode(cls, v):
        if not re.match(r"^\d{6}$", v):
            raise ValueError("Pincode must be exactly 6 digits")
        return v

    @field_validator("full_address")
    @classmethod
    def validate_full_address(cls, v):
        if not v or len(v.strip()) < 5:
            raise ValueError("Full address must be at least 5 characters")
        return v.strip()

    @field_validator("city")
    @classmethod
    def validate_city(cls, v):
        if not v or len(v.strip()) < 2:
            raise ValueError("City name must be at least 2 characters")
        return v.strip()

    @field_validator("recipient_phone")
    @classmethod
    def validate_recipient_phone(cls, v):
        if v is not None and v != "" and not re.match(r"^\+?\d{10,13}$", v):
            raise ValueError("Invalid phone number format")
        return v


class AddressResponse(BaseModel):
    id: str
    label: str
    recipient_name: Optional[str] = None
    recipient_phone: Optional[str] = None
    house_number: Optional[str] = None
    street: Optional[str] = None
    landmark: Optional[str] = None
    full_address: str
    city: str
    state: Optional[str] = None
    pincode: str
    lat: Optional[float] = None
    lng: Optional[float] = None
    is_default: bool

    class Config:
        from_attributes = True
