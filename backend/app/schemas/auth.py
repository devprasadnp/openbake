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
    recipient_name: str
    recipient_phone: str
    house_number: str
    street: str
    landmark: Optional[str] = None
    full_address: str
    city: str
    state: str
    pincode: str
    lat: Optional[float] = None
    lng: Optional[float] = None
    is_default: bool = False

    @field_validator("label", "recipient_name", "house_number", "street", "state")
    @classmethod
    def validate_required_text_fields(cls, v: str):
        value = v.strip()
        if not value:
            raise ValueError("This field is required")
        return value

    @field_validator("pincode")
    @classmethod
    def validate_pincode(cls, v):
        digits = re.sub(r"\D", "", v)
        if not re.match(r"^\d{6}$", digits):
            raise ValueError("Pincode must be exactly 6 digits")
        return digits

    @field_validator("full_address")
    @classmethod
    def validate_full_address(cls, v):
        value = v.strip()
        if not value or len(value) < 5:
            raise ValueError("Full address must be at least 5 characters")
        return value

    @field_validator("city")
    @classmethod
    def validate_city(cls, v):
        value = v.strip()
        if not value or len(value) < 2:
            raise ValueError("City name must be at least 2 characters")
        return value

    @field_validator("state")
    @classmethod
    def validate_state(cls, v):
        value = v.strip()
        if not value or len(value) < 2:
            raise ValueError("State name must be at least 2 characters")
        return value

    @field_validator("recipient_phone")
    @classmethod
    def validate_recipient_phone(cls, v):
        digits = re.sub(r"\D", "", v)
        if not re.match(r"^[6-9]\d{9}$", digits):
            raise ValueError("Recipient phone must be a valid 10-digit Indian mobile number")
        return digits


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
