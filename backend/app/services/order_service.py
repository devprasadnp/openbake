from decimal import Decimal
from typing import List

from sqlalchemy.orm import Session

from app.models.order import Order, OrderItem
from app.models.product import Product
from app.models.coupon import Coupon
from app.schemas.order import OrderCreate


def calculate_order_totals(
    db: Session,
    items: list,
    coupon_code: str = None,
    order_type: str = "delivery",
) -> dict:
    """Calculate subtotal, discount, delivery fee, and total for an order.

    Uses SELECT FOR UPDATE to prevent race conditions on stock.
    """
    subtotal = Decimal("0.00")
    validated_items = []

    for item in items:
        # Lock the product row to prevent concurrent over-ordering
        product = (
            db.query(Product)
            .filter(Product.id == item.product_id)
            .with_for_update()
            .first()
        )
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
                "quantity": item.quantity,
                "unit_price": float(unit_price),
                "customization": item.customization,
            }
        )

    # Apply coupon
    discount = Decimal("0.00")
    if coupon_code:
        coupon = (
            db.query(Coupon)
            .filter(Coupon.code == coupon_code, Coupon.is_active == True)
            .with_for_update()
            .first()
        )
        if coupon and float(subtotal) >= float(coupon.min_order_value):
            if coupon.discount_type == "flat":
                discount = Decimal(str(coupon.discount_value))
            elif coupon.discount_type == "percent":
                discount = subtotal * Decimal(str(coupon.discount_value)) / Decimal("100")
            coupon.used_count += 1

    # Delivery fee
    delivery_fee = Decimal("40.00") if order_type == "delivery" else Decimal("0.00")

    total = max(subtotal - discount + delivery_fee, Decimal("0.00"))

    return {
        "subtotal": float(subtotal),
        "discount": float(discount),
        "delivery_fee": float(delivery_fee),
        "total": float(total),
        "validated_items": validated_items,
    }


def place_order(db: Session, user_id: str, data: OrderCreate) -> Order:
    """Create a new order with items and atomically decrement stock."""
    totals = calculate_order_totals(
        db, data.items, data.coupon_code, data.order_type
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
        payment_status="pending",
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

        # Decrement stock (row is already locked via SELECT FOR UPDATE above)
        product = db.query(Product).filter(Product.id == item_data["product_id"]).first()
        if product:
            product.stock_count = max(0, product.stock_count - item_data["quantity"])

    db.commit()
    db.refresh(order)
    return order


def restore_stock_for_order(db: Session, order: Order) -> None:
    """Restore product stock when an order is cancelled."""
    for item in order.items:
        product = db.query(Product).filter(Product.id == item.product_id).with_for_update().first()
        if product:
            product.stock_count += item.quantity
