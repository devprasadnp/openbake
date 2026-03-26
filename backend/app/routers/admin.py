from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.order import Order
from app.models.product import Category, Product, ProductVariant
from app.models.coupon import Coupon
from app.models.user import User
from app.schemas.product import (
    ProductCreate, ProductUpdate, ProductResponse,
    CategoryCreate, CategoryResponse,
)
from app.schemas.order import OrderResponse, OrderStatusUpdate, CouponCreate, CouponResponse
from app.utils.jwt import require_admin

router = APIRouter()


# --- Dashboard ---

@router.get("/dashboard")
def admin_dashboard(
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Sales summary — today, this week, this month."""
    now = datetime.now(timezone.utc)
    today_start = now.replace(hour=0, minute=0, second=0, microsecond=0)
    week_start = today_start - timedelta(days=today_start.weekday())
    month_start = today_start.replace(day=1)

    def revenue_since(since):
        result = (
            db.query(func.sum(Order.total))
            .filter(Order.created_at >= since, Order.payment_status == "paid")
            .scalar()
        )
        return float(result or 0)

    def orders_since(since):
        return db.query(Order).filter(Order.created_at >= since).count()

    return {
        "today_orders": orders_since(today_start),
        "today_revenue": revenue_since(today_start),
        "week_orders": orders_since(week_start),
        "week_revenue": revenue_since(week_start),
        "month_orders": orders_since(month_start),
        "month_revenue": revenue_since(month_start),
        "pending_orders": db.query(Order).filter(Order.status == "placed").count(),
    }


# --- Orders Management ---

@router.get("/orders", response_model=list[OrderResponse])
def admin_list_orders(
    status_filter: str = Query(None),
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """List all orders, optionally filtered by status."""
    query = db.query(Order).order_by(Order.created_at.desc())
    if status_filter:
        query = query.filter(Order.status == status_filter)
    return query.all()


@router.patch("/orders/{order_id}", response_model=OrderResponse)
def admin_update_order_status(
    order_id: str,
    data: OrderStatusUpdate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Update order status."""
    order = db.query(Order).filter(Order.id == str(order_id)).first()
    if not order:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Order not found")

    valid_transitions = {
        "placed": ["accepted", "cancelled"],
        "accepted": ["preparing", "cancelled"],
        "preparing": ["dispatched"],
        "dispatched": ["delivered"],
    }
    allowed = valid_transitions.get(order.status, [])
    if data.status not in allowed:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Cannot transition from '{order.status}' to '{data.status}'",
        )

    order.status = data.status
    db.commit()
    db.refresh(order)
    return order


# --- Products Management ---

@router.get("/products", response_model=list[ProductResponse])
def admin_list_products(
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """List all products (including unavailable)."""
    return db.query(Product).all()


@router.post("/products", response_model=ProductResponse)
def admin_create_product(
    data: ProductCreate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Create a new product."""
    product = Product(
        category_id=data.category_id,
        name=data.name,
        description=data.description,
        price=data.price,
        is_available=data.is_available,
        is_eggless_available=data.is_eggless_available,
        customizable=data.customizable,
        stock_count=data.stock_count,
    )
    product.images = data.images
    db.add(product)
    db.flush()

    for variant in data.variants:
        db.add(ProductVariant(
            product_id=product.id,
            variant_type=variant.variant_type,
            value=variant.value,
            extra_price=variant.extra_price,
        ))

    db.commit()
    db.refresh(product)
    return product


@router.patch("/products/{product_id}", response_model=ProductResponse)
def admin_update_product(
    product_id: str,
    data: ProductUpdate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Update a product."""
    product = db.query(Product).filter(Product.id == product_id).first()
    if not product:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Product not found")

    for field, value in data.model_dump(exclude_unset=True).items():
        if field == "images":
            product.images = value
        else:
            setattr(product, field, value)

    db.commit()
    db.refresh(product)
    return product


@router.delete("/products/{product_id}")
def admin_delete_product(
    product_id: str,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Delete a product."""
    product = db.query(Product).filter(Product.id == product_id).first()
    if not product:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Product not found")
    db.delete(product)
    db.commit()
    return {"message": "Product deleted"}


# --- Inventory ---

@router.get("/inventory")
def admin_inventory(
    threshold: int = Query(10),
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """List products with low stock."""
    products = db.query(Product).filter(Product.stock_count < threshold).all()
    return [
        {
            "id": str(p.id),
            "name": p.name,
            "stock_count": p.stock_count,
            "is_available": p.is_available,
        }
        for p in products
    ]


# --- Categories ---

@router.post("/categories", response_model=CategoryResponse)
def admin_create_category(
    data: CategoryCreate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Create a new category."""
    category = Category(**data.model_dump())
    db.add(category)
    db.commit()
    db.refresh(category)
    return category


# --- Coupons ---

@router.get("/coupons", response_model=list[CouponResponse])
def admin_list_coupons(
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """List all coupons."""
    return db.query(Coupon).all()


@router.post("/coupons", response_model=CouponResponse)
def admin_create_coupon(
    data: CouponCreate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Create a new coupon."""
    coupon = Coupon(**data.model_dump())
    db.add(coupon)
    db.commit()
    db.refresh(coupon)
    return coupon


@router.patch("/coupons/{coupon_id}", response_model=CouponResponse)
def admin_update_coupon(
    coupon_id: str,
    data: CouponCreate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Update a coupon."""
    coupon = db.query(Coupon).filter(Coupon.id == coupon_id).first()
    if not coupon:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Coupon not found")
    for field, value in data.model_dump(exclude_unset=True).items():
        setattr(coupon, field, value)
    db.commit()
    db.refresh(coupon)
    return coupon
