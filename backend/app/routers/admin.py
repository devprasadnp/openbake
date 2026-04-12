from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.database import get_db
from app.models.order import Order, OrderItem
from app.models.product import Category, Product, ProductVariant
from app.models.coupon import Coupon
from app.models.user import User
from app.schemas.product import (
    ProductCreate, ProductUpdate, ProductResponse,
    CategoryCreate, CategoryResponse,
)
from app.schemas.order import OrderResponse, OrderStatusUpdate, AdminOrderDetailResponse, CouponCreate, CouponResponse
from app.utils.jwt import require_admin
from app.services.waitlist_service import notify_waitlist_users

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


# --- Analytics ---

@router.get("/analytics")
def admin_analytics(
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Extended analytics: daily revenue/orders trend, status breakdown, top products, order type split."""
    now = datetime.now(timezone.utc)

    # ── Daily trend (last 7 days) ──────────────────────────────────────────
    daily = []
    for i in range(6, -1, -1):
        day_start = (now - timedelta(days=i)).replace(hour=0, minute=0, second=0, microsecond=0)
        day_end = day_start + timedelta(days=1)
        orders_count = (
            db.query(Order)
            .filter(Order.created_at >= day_start, Order.created_at < day_end)
            .count()
        )
        revenue = (
            db.query(func.sum(Order.total))
            .filter(
                Order.created_at >= day_start,
                Order.created_at < day_end,
                Order.payment_status == "paid",
            )
            .scalar()
        ) or 0.0
        daily.append({
            "date": day_start.strftime("%b %d"),
            "orders": orders_count,
            "revenue": float(revenue),
        })

    # ── Orders by status ───────────────────────────────────────────────────
    status_rows = (
        db.query(Order.status, func.count(Order.id))
        .group_by(Order.status)
        .all()
    )
    status_breakdown = [{"status": s, "count": c} for s, c in status_rows]

    # ── Order type split ──────────────────────────────────────────────────
    type_rows = (
        db.query(Order.order_type, func.count(Order.id))
        .group_by(Order.order_type)
        .all()
    )
    order_type_split = [{"type": t, "count": c} for t, c in type_rows]

    # ── Top 5 products by units sold ─────────────────────────────────────
    top_products_rows = (
        db.query(
            Product.name,
            func.sum(OrderItem.quantity).label("units"),
            func.sum(OrderItem.unit_price * OrderItem.quantity).label("revenue"),
        )
        .join(OrderItem, OrderItem.product_id == Product.id)
        .group_by(Product.id, Product.name)
        .order_by(func.sum(OrderItem.quantity).desc())
        .limit(5)
        .all()
    )
    top_products = [
        {"name": name, "units": int(units or 0), "revenue": float(rev or 0)}
        for name, units, rev in top_products_rows
    ]

    # ── Payment method split ──────────────────────────────────────────────
    payment_rows = (
        db.query(Order.payment_method, func.count(Order.id))
        .filter(Order.payment_method.isnot(None))
        .group_by(Order.payment_method)
        .all()
    )
    payment_split = [{"method": m, "count": c} for m, c in payment_rows]

    return {
        "daily_trend": daily,
        "status_breakdown": status_breakdown,
        "order_type_split": order_type_split,
        "top_products": top_products,
        "payment_split": payment_split,
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
    # Record status timestamp
    from datetime import datetime, timezone
    timestamps = order.status_timestamps or {}
    timestamps[data.status] = datetime.now(timezone.utc).isoformat()
    order.status_timestamps = timestamps
    db.commit()
    db.refresh(order)
    return order


@router.get("/orders/{order_id}", response_model=AdminOrderDetailResponse)
def admin_get_order_detail(
    order_id: str,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Get full order details including customer info for admin."""
    order = db.query(Order).filter(Order.id == str(order_id)).first()
    if not order:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Order not found")

    # Build enriched response with customer details
    order_data = AdminOrderDetailResponse.model_validate(order)
    if order.user:
        order_data.customer = {
            "id": order.user.id,
            "name": order.user.name,
            "email": order.user.email,
            "phone": order.user.phone,
            "profile_image_url": order.user.profile_image_url,
        }
    return order_data


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


@router.patch("/inventory/{product_id}")
def admin_update_stock(
    product_id: str,
    stock_count: int = Query(..., ge=0),
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Update product stock count. Triggers waitlist notifications on restock."""
    product = db.query(Product).filter(Product.id == product_id).first()
    if not product:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Product not found")

    old_stock = product.stock_count
    product.stock_count = stock_count

    # Auto-enable availability if restocked
    if stock_count > 0 and not product.is_available:
        product.is_available = True

    db.commit()
    db.refresh(product)

    # Notify waitlist users if product went from 0 → positive stock
    notified = 0
    if old_stock == 0 and stock_count > 0:
        notified = notify_waitlist_users(db, product_id)

    return {
        "id": str(product.id),
        "name": product.name,
        "stock_count": product.stock_count,
        "is_available": product.is_available,
        "waitlist_notified": notified,
    }


# --- Categories ---

@router.get("/categories", response_model=list[CategoryResponse])
def admin_list_categories(
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """List all categories."""
    return db.query(Category).all()


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


@router.patch("/categories/{category_id}", response_model=CategoryResponse)
def admin_update_category(
    category_id: str,
    data: CategoryCreate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Update a category."""
    category = db.query(Category).filter(Category.id == category_id).first()
    if not category:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Category not found")
    for field, value in data.model_dump(exclude_unset=True).items():
        setattr(category, field, value)
    db.commit()
    db.refresh(category)
    return category


@router.delete("/categories/{category_id}")
def admin_delete_category(
    category_id: str,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Delete a category."""
    category = db.query(Category).filter(Category.id == category_id).first()
    if not category:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Category not found")
    db.delete(category)
    db.commit()
    return {"message": "Category deleted"}


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
