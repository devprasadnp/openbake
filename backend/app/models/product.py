import uuid
import json
from sqlalchemy import String, Boolean, Integer, Float, Text, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.database import Base


class Category(Base):
    __tablename__ = "categories"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    name: Mapped[str] = mapped_column(String(100), nullable=False, unique=True)
    image_url: Mapped[str] = mapped_column(String(500), nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)

    # Relationships
    products = relationship("Product", back_populates="category", cascade="all, delete-orphan")


class Product(Base):
    __tablename__ = "products"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    category_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("categories.id"), nullable=False
    )
    name: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=True)
    price: Mapped[float] = mapped_column(Float, nullable=False)
    # Store images as JSON string for SQLite compatibility
    _images: Mapped[str] = mapped_column("images", Text, default="[]")
    is_available: Mapped[bool] = mapped_column(Boolean, default=True)
    is_eggless_available: Mapped[bool] = mapped_column(Boolean, default=False)
    customizable: Mapped[bool] = mapped_column(Boolean, default=False)
    stock_count: Mapped[int] = mapped_column(Integer, default=0)
    unlimited_stock: Mapped[bool] = mapped_column(Boolean, default=False)
    rating: Mapped[float] = mapped_column(Float, default=0.0)

    @property
    def images(self) -> list:
        try:
            return json.loads(self._images) if self._images else []
        except (json.JSONDecodeError, TypeError):
            return []

    @images.setter
    def images(self, value: list):
        self._images = json.dumps(value) if value else "[]"

    # Relationships
    category = relationship("Category", back_populates="products")
    variants = relationship("ProductVariant", back_populates="product", cascade="all, delete-orphan")
    reviews = relationship("Review", back_populates="product", cascade="all, delete-orphan")
    wishlist_items = relationship("WishlistItem", back_populates="product", cascade="all, delete-orphan")


class ProductVariant(Base):
    __tablename__ = "product_variants"

    id: Mapped[str] = mapped_column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    product_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("products.id"), nullable=False
    )
    variant_type: Mapped[str] = mapped_column(
        String(50), nullable=False
    )  # size | flavor
    value: Mapped[str] = mapped_column(
        String(100), nullable=False
    )  # e.g. 0.5kg, 1kg | chocolate, vanilla
    extra_price: Mapped[float] = mapped_column(Float, default=0.0)

    # Relationships
    product = relationship("Product", back_populates="variants")
