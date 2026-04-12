from pydantic import BaseModel, Field, field_validator
from typing import Optional, List, Dict, Any
from datetime import date, datetime
from app.schemas.auth import AddressResponse


# --- Order Item Schemas ---

class OrderItemCreate(BaseModel):
    product_id: str
    quantity: int = Field(default=1, ge=1, le=50, description="Quantity between 1 and 50")
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
    idempotency_key: Optional[str] = None  # Client-generated UUID to prevent duplicate orders

    @field_validator("address_id", mode="before")
    @classmethod
    def normalize_address_id(cls, v):
        """Convert empty string to None so FK constraint doesn't fail."""
        if v == "" or v is None:
            return None
        return v

    @field_validator("payment_method")
    @classmethod
    def validate_payment_method(cls, v):
        allowed = {"upi", "card", "wallet", "cod"}
        if v not in allowed:
            raise ValueError(f"Invalid payment method '{v}'. Must be one of: {', '.join(sorted(allowed))}")
        return v

    @field_validator("order_type")
    @classmethod
    def validate_order_type(cls, v):
        if v not in ("delivery", "pickup"):
            raise ValueError("order_type must be 'delivery' or 'pickup'")
        return v

    @field_validator("items")
    @classmethod
    def validate_items_not_empty(cls, v):
        if not v or len(v) == 0:
            raise ValueError("Order must contain at least one item")
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
    idempotency_key: Optional[str] = None
    estimated_delivery_minutes: Optional[int] = None
    scheduled_date: Optional[date] = None
    time_slot: Optional[str] = None
    special_note: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    status_timestamps: Optional[Dict[str, Any]] = None
    items: List[OrderItemResponse] = []

    class Config:
        from_attributes = True


class OrderCustomerInfo(BaseModel):
    """Minimal user info exposed to admin for order context."""
    id: str
    name: Optional[str] = None
    email: Optional[str] = None
    phone: Optional[str] = None
    profile_image_url: Optional[str] = None

    class Config:
        from_attributes = True


class AdminOrderDetailResponse(OrderResponse):
    """Enriched order response that includes customer details for admin views."""
    customer: Optional[OrderCustomerInfo] = None


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
