import uuid
import json
from datetime import datetime, date
from sqlalchemy import (
    String, Integer, Float, Text, ForeignKey, DateTime, Date, func,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database import Base


class Order(Base):
    __tablename__ = "orders"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=False
    )
    address_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("addresses.id"), nullable=True
    )
    order_type: Mapped[str] = mapped_column(
        String(20), nullable=False, default="delivery"
    )  # delivery | pickup
    status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="placed"
    )  # placed | accepted | preparing | dispatched | delivered | cancelled
    subtotal: Mapped[float] = mapped_column(Float, nullable=False)
    discount: Mapped[float] = mapped_column(Float, default=0.0)
    delivery_fee: Mapped[float] = mapped_column(Float, default=0.0)
    total: Mapped[float] = mapped_column(Float, nullable=False)
    coupon_code: Mapped[str] = mapped_column(String(50), nullable=True)
    payment_method: Mapped[str] = mapped_column(String(50), nullable=True)
    payment_status: Mapped[str] = mapped_column(
        String(20), nullable=False, default="pending"
    )  # pending | paid | failed
    razorpay_order_id: Mapped[str] = mapped_column(String(100), nullable=True)
    razorpay_payment_id: Mapped[str] = mapped_column(String(100), nullable=True)
    estimated_delivery_minutes: Mapped[int] = mapped_column(Integer, nullable=True)
    scheduled_date: Mapped[date] = mapped_column(Date, nullable=True)
    time_slot: Mapped[str] = mapped_column(String(50), nullable=True)
    special_note: Mapped[str] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now()
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime, server_default=func.now(), onupdate=func.now()
    )

    # Relationships
    user = relationship("User", back_populates="orders")
    address = relationship("Address", lazy="joined")
    items = relationship("OrderItem", back_populates="order", cascade="all, delete-orphan")


class OrderItem(Base):
    __tablename__ = "order_items"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    order_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("orders.id"), nullable=False
    )
    product_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("products.id"), nullable=False
    )
    quantity: Mapped[int] = mapped_column(Integer, nullable=False, default=1)
    unit_price: Mapped[float] = mapped_column(Float, nullable=False)
    # Store customization as JSON string for SQLite compatibility
    _customization: Mapped[str] = mapped_column("customization", Text, nullable=True)

    @property
    def customization(self) -> dict | None:
        try:
            return json.loads(self._customization) if self._customization else None
        except (json.JSONDecodeError, TypeError):
            return None

    @customization.setter
    def customization(self, value):
        self._customization = json.dumps(value) if value else None

    # Relationships
    order = relationship("Order", back_populates="items")
    product = relationship("Product")

    @property
    def product_name(self) -> str | None:
        return self.product.name if self.product else None
