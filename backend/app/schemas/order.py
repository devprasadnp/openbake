from pydantic import BaseModel, Field, field_validator
from typing import Optional, List, Dict, Any
from datetime import date, datetime
from app.schemas.auth import AddressResponse


# --- Order Item Schemas ---

class OrderItemCreate(BaseModel):
    product_id: str
    quantity: int = 1
    customization: Optional[Dict[str, Any]] = None  # {eggless, size, flavor, cake_message}


class OrderItemResponse(BaseModel):
    id: str
    product_id: str
    product_name: Optional[str] = None
    quantity: int
    unit_price: float
    customization: Optional[Dict[str, Any]] = None

    class Config:
        from_attributes = True


# --- Order Schemas ---

class OrderCreate(BaseModel):
    address_id: Optional[str] = None
    order_type: str = "delivery"  # delivery | pickup
    items: List[OrderItemCreate]
    coupon_code: Optional[str] = None
    payment_method: str  # upi | card | wallet | cod
    scheduled_date: Optional[date] = None
    time_slot: Optional[str] = None
    special_note: Optional[str] = None

    @field_validator("address_id", mode="before")
    @classmethod
    def normalize_address_id(cls, v):
        """Convert empty string to None so FK constraint doesn't fail."""
        if v == "" or v is None:
            return None
        return v


class OrderResponse(BaseModel):
    id: str
    user_id: str
    address_id: Optional[str] = None
    address: Optional[AddressResponse] = None
    order_type: str
    status: str
    subtotal: float
    discount: float
    delivery_fee: float
    total: float
    coupon_code: Optional[str] = None
    payment_method: Optional[str] = None
    payment_status: str
    razorpay_order_id: Optional[str] = None
    razorpay_payment_id: Optional[str] = None
    estimated_delivery_minutes: Optional[int] = None
    scheduled_date: Optional[date] = None
    time_slot: Optional[str] = None
    special_note: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    items: List[OrderItemResponse] = []

    class Config:
        from_attributes = True


class OrderStatusUpdate(BaseModel):
    status: str  # placed | accepted | preparing | dispatched | delivered | cancelled


# --- Coupon Schemas ---

class CouponCreate(BaseModel):
    code: str
    discount_type: str  # flat | percent
    discount_value: float
    min_order_value: float = 0.0
    max_uses: int = 100
    valid_from: date
    valid_until: date
    is_active: bool = True


class CouponResponse(BaseModel):
    id: str
    code: str
    discount_type: str
    discount_value: float
    min_order_value: float
    max_uses: int
    used_count: int
    valid_from: date
    valid_until: date
    is_active: bool

    class Config:
        from_attributes = True


class CouponApplyRequest(BaseModel):
    code: str
    subtotal: float


class CouponApplyResponse(BaseModel):
    valid: bool
    discount: float = 0.0
    message: str = ""


# --- Review Schemas ---

class ReviewCreate(BaseModel):
    product_id: str
    order_id: str
    rating: int = Field(..., ge=1, le=5, description="Rating from 1 to 5")
    comment: Optional[str] = None


class ReviewResponse(BaseModel):
    id: str
    user_id: str
    product_id: str
    order_id: str
    rating: int
    comment: Optional[str] = None
    created_at: datetime

    class Config:
        from_attributes = True


# --- Cart Validation ---

class CartItemValidate(BaseModel):
    product_id: str
    quantity: int
    customization: Optional[Dict[str, Any]] = None


class CartValidateRequest(BaseModel):
    items: List[CartItemValidate]


class CartValidateResponse(BaseModel):
    valid: bool
    subtotal: float
    items: List[Dict[str, Any]]
    errors: List[str] = []
