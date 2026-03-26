from pydantic import BaseModel
from typing import Optional, List


# --- Category Schemas ---

class CategoryResponse(BaseModel):
    id: str
    name: str
    image_url: Optional[str] = None
    is_active: bool

    class Config:
        from_attributes = True


class CategoryCreate(BaseModel):
    name: str
    image_url: Optional[str] = None
    is_active: bool = True


# --- Product Variant Schemas ---

class ProductVariantResponse(BaseModel):
    id: str
    variant_type: str
    value: str
    extra_price: float

    class Config:
        from_attributes = True


class ProductVariantCreate(BaseModel):
    variant_type: str  # size | flavor
    value: str
    extra_price: float = 0.0


# --- Product Schemas ---

class ProductResponse(BaseModel):
    id: str
    category_id: str
    name: str
    description: Optional[str] = None
    price: float
    images: List[str] = []
    is_available: bool
    is_eggless_available: bool
    customizable: bool
    stock_count: int
    rating: float
    variants: List[ProductVariantResponse] = []

    class Config:
        from_attributes = True


class ProductCreate(BaseModel):
    category_id: str
    name: str
    description: Optional[str] = None
    price: float
    images: List[str] = []
    is_available: bool = True
    is_eggless_available: bool = False
    customizable: bool = False
    stock_count: int = 0
    variants: List[ProductVariantCreate] = []


class ProductUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    price: Optional[float] = None
    images: Optional[List[str]] = None
    is_available: Optional[bool] = None
    is_eggless_available: Optional[bool] = None
    customizable: Optional[bool] = None
    stock_count: Optional[int] = None


class ProductListParams(BaseModel):
    category_id: Optional[str] = None
    search: Optional[str] = None
    min_price: Optional[float] = None
    max_price: Optional[float] = None
    min_rating: Optional[float] = None
    eggless_only: bool = False
    page: int = 1
    page_size: int = 20
