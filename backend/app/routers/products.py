from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.product import Category, Product
from app.models.review import Review
from app.schemas.product import CategoryResponse, ProductResponse, PaginatedProductResponse
from app.schemas.order import ReviewResponse

router = APIRouter()


@router.get("/categories", response_model=list[CategoryResponse])
def list_categories(db: Session = Depends(get_db)):
    """List all active categories."""
    return db.query(Category).filter(Category.is_active == True).all()


@router.get("/products", response_model=PaginatedProductResponse)
def list_products(
    category_id: Optional[str] = Query(None),
    search: Optional[str] = Query(None),
    min_price: Optional[float] = Query(None),
    max_price: Optional[float] = Query(None),
    min_rating: Optional[float] = Query(None),
    eggless_only: bool = Query(False),
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """List products with optional filters. Returns paginated results with metadata."""
    query = db.query(Product).filter(Product.is_available == True)

    if category_id:
        query = query.filter(Product.category_id == category_id)
    if search:
        query = query.filter(Product.name.ilike(f"%{search}%"))
    if min_price is not None:
        query = query.filter(Product.price >= min_price)
    if max_price is not None:
        query = query.filter(Product.price <= max_price)
    if min_rating is not None:
        query = query.filter(Product.rating >= min_rating)
    if eggless_only:
        query = query.filter(Product.is_eggless_available == True)

    total = query.count()
    offset = (page - 1) * page_size
    items = query.offset(offset).limit(page_size).all()

    return {
        "items": items,
        "total": total,
        "page": page,
        "page_size": page_size,
        "pages": max(1, -(-total // page_size)),  # ceiling division
        "has_next": (page * page_size) < total,
        "has_prev": page > 1,
    }


@router.get("/products/{product_id}", response_model=ProductResponse)
def get_product(product_id: str, db: Session = Depends(get_db)):
    """Get a single product by ID."""
    product = db.query(Product).filter(Product.id == product_id).first()
    if not product:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Product not found")
    return product


@router.get("/products/{product_id}/reviews", response_model=list[ReviewResponse])
def get_product_reviews(
    product_id: str,
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
):
    """Get reviews for a product."""
    offset = (page - 1) * page_size
    return (
        db.query(Review)
        .filter(Review.product_id == product_id)
        .order_by(Review.created_at.desc())
        .offset(offset)
        .limit(page_size)
        .all()
    )
