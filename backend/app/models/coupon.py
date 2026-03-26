import uuid
from datetime import date
from sqlalchemy import String, Boolean, Integer, Float, Date
from sqlalchemy.orm import Mapped, mapped_column
from app.database import Base


class Coupon(Base):
    __tablename__ = "coupons"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    code: Mapped[str] = mapped_column(String(50), unique=True, nullable=False)
    discount_type: Mapped[str] = mapped_column(
        String(20), nullable=False
    )  # flat | percent
    discount_value: Mapped[float] = mapped_column(Float, nullable=False)
    min_order_value: Mapped[float] = mapped_column(Float, default=0.0)
    max_uses: Mapped[int] = mapped_column(Integer, default=100)
    used_count: Mapped[int] = mapped_column(Integer, default=0)
    valid_from: Mapped[date] = mapped_column(Date, nullable=False)
    valid_until: Mapped[date] = mapped_column(Date, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)
