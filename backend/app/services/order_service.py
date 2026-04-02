from decimal import Decimal
from typing import List

from sqlalchemy.orm import Session

from app.models.order import Order, OrderItem
from app.models.product import Product
from app.models.user import Address
from app.models.coupon import Coupon
from app.schemas.order import OrderCreate
from app.services.delivery_service import calculate_delivery_fee
from app.config import get_settings

settings = get_settings()
is_sqlite = settings.DATABASE_URL.startswith("sqlite")


def calculate_order_totals(
    db: Session,
    items: list,
    coupon_code: str = None,
    order_type: str = "delivery",
    address_id: str = None,
    user_id: str = None,
) -> dict:
    """Calculate subtotal, discount, delivery fee, and total for an order.

    Uses SELECT FOR UPDATE on PostgreSQL to prevent race conditions on stock.
    For SQLite, this is a no-op (SQLite uses DB-level locking).
    """
    subtotal = Decimal("0.00")
    validated_items = []

    for item in items:
        # Lock the product row to prevent concurrent over-ordering (PostgreSQL only)
        query = db.query(Product).filter(Product.id == item.product_id)
        if not is_sqlite:
            query = query.with_for_update()
        product = query.first()

        if not product:
            raise ValueError(f"Product {item.product_id} not found")
        if not product.is_available:
            raise ValueError(f"Product '{product.name}' is currently unavailable")
        if product.stock_count < item.quantity:
            raise ValueError(
                f"Only {product.stock_count} units left for '{product.name}'"
            )

        unit_price = Decimal(str(product.price))
        line_total = unit_price * item.quantity
        subtotal += line_total

        validated_items.append(
            {
                "product_id": product.id,
                "product_name": product.name,
                "quantity": item.quantity,
                "unit_price": float(unit_price),
                "stock_count": product.stock_count,
                "is_available": product.is_available,
                "customization": item.customization,
            }
        )

    # Apply coupon
    discount = Decimal("0.00")
    if coupon_code:
        coupon_query = db.query(Coupon).filter(Coupon.code == coupon_code, Coupon.is_active == True)
        if not is_sqlite:
            coupon_query = coupon_query.with_for_update()
        coupon = coupon_query.first()

        if coupon and float(subtotal) >= float(coupon.min_order_value):
            if coupon.discount_type == "flat":
                discount = Decimal(str(coupon.discount_value))
            elif coupon.discount_type == "percent":
                discount = subtotal * Decimal(str(coupon.discount_value)) / Decimal("100")
            coupon.used_count += 1

    # Delivery fee — distance-based calculation
    delivery_fee = Decimal("0.00")
    estimated_delivery_minutes = None
    if order_type == "delivery":
        address = None
        if address_id:
            addr_query = db.query(Address).filter(Address.id == address_id)
            if user_id:
                addr_query = addr_query.filter(Address.user_id == user_id)
            address = addr_query.first()
            if not address:
                raise ValueError("Address not found or does not belong to you.")

        if address and address.lat and address.lng:
            delivery_info = calculate_delivery_fee(db, address.lat, address.lng)
            delivery_fee = Decimal(str(delivery_info["delivery_fee"]))
            estimated_delivery_minutes = delivery_info["estimated_time_minutes"]
            if not delivery_info["is_deliverable"]:
                raise ValueError("Delivery address is too far. Maximum delivery radius is 25 km.")
        else:
            # Fallback to flat fee if no coordinates
            delivery_fee = Decimal("40.00")

    total = max(subtotal - discount + delivery_fee, Decimal("0.00"))

    return {
        "subtotal": float(subtotal),
        "discount": float(discount),
        "delivery_fee": float(delivery_fee),
        "total": float(total),
        "validated_items": validated_items,
        "estimated_delivery_minutes": estimated_delivery_minutes,
    }


def place_order(db: Session, user_id: str, data: OrderCreate) -> Order:
    """Create a new order with items and atomically decrement stock."""
    totals = calculate_order_totals(
        db, data.items, data.coupon_code, data.order_type, data.address_id, user_id
    )

    order = Order(
        user_id=user_id,
        address_id=data.address_id,
        order_type=data.order_type,
        status="placed",
        subtotal=totals["subtotal"],
        discount=totals["discount"],
        delivery_fee=totals["delivery_fee"],
        total=totals["total"],
        coupon_code=data.coupon_code,
        payment_method=data.payment_method,
        payment_status="paid" if data.payment_method == "cod" else "pending",
        estimated_delivery_minutes=totals.get("estimated_delivery_minutes"),
        scheduled_date=data.scheduled_date,
        time_slot=data.time_slot,
        special_note=data.special_note,
    )
    db.add(order)
    db.flush()  # get order.id

    for item_data in totals["validated_items"]:
        order_item = OrderItem(
            order_id=order.id,
            product_id=item_data["product_id"],
            quantity=item_data["quantity"],
            unit_price=item_data["unit_price"],
        )
        order_item.customization = item_data["customization"]
        db.add(order_item)

        # Decrement stock
        product = db.query(Product).filter(Product.id == item_data["product_id"]).first()
        if product:
            product.stock_count = max(0, product.stock_count - item_data["quantity"])

    db.commit()
    db.refresh(order)
    return order


def restore_stock_for_order(db: Session, order: Order) -> None:
    """Restore product stock when an order is cancelled."""
    for item in order.items:
        query = db.query(Product).filter(Product.id == item.product_id)
        if not is_sqlite:
            query = query.with_for_update()
        product = query.first()
        if product:
            product.stock_count += item.quantity
