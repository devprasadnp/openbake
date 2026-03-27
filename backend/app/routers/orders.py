from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError

from app.database import get_db
from app.models.order import Order
from app.models.user import User
from app.models.coupon import Coupon
from app.schemas.order import (
    OrderCreate, OrderResponse, CartValidateRequest, CartValidateResponse,
    CouponApplyRequest, CouponApplyResponse,
)
from app.services.order_service import place_order, calculate_order_totals
from app.utils.jwt import get_current_user

router = APIRouter()


@router.post("/orders", response_model=OrderResponse)
def create_order(
    data: OrderCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Place a new order."""
    try:
        order = place_order(db, current_user.id, data)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    except IntegrityError as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid address or order data. Please check your delivery address.",
        )
    except Exception as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to place order: {str(e)}",
        )
    return order


@router.get("/orders", response_model=list[OrderResponse])
def list_orders(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get order history for the current user."""
    return (
        db.query(Order)
        .filter(Order.user_id == current_user.id)
        .order_by(Order.created_at.desc())
        .all()
    )


@router.get("/orders/{order_id}", response_model=OrderResponse)
def get_order(
    order_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get order details by ID."""
    order = (
        db.query(Order)
        .filter(Order.id == order_id, Order.user_id == current_user.id)
        .first()
    )
    if not order:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Order not found")
    return order


@router.patch("/orders/{order_id}/cancel", response_model=OrderResponse)
def cancel_order(
    order_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Cancel an order (only if status is 'placed')."""
    order = (
        db.query(Order)
        .filter(Order.id == order_id, Order.user_id == current_user.id)
        .first()
    )
    if not order:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Order not found")
    if order.status != "placed":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Order can only be cancelled when status is 'placed'",
        )
    order.status = "cancelled"
    db.commit()
    db.refresh(order)
    return order


@router.post("/cart/validate", response_model=CartValidateResponse)
def validate_cart(
    data: CartValidateRequest,
    db: Session = Depends(get_db),
):
    """Validate cart items (prices, availability) before checkout."""
    try:
        totals = calculate_order_totals(db, data.items)
        return CartValidateResponse(
            valid=True,
            subtotal=totals["subtotal"],
            items=totals["validated_items"],
        )
    except ValueError as e:
        return CartValidateResponse(
            valid=False,
            subtotal=0,
            items=[],
            errors=[str(e)],
        )


@router.post("/coupons/apply", response_model=CouponApplyResponse)
def apply_coupon(
    data: CouponApplyRequest,
    db: Session = Depends(get_db),
):
    """Validate and apply a coupon code."""
    from datetime import date as date_type

    coupon = db.query(Coupon).filter(
        Coupon.code == data.code.upper(),
        Coupon.is_active == True,
    ).first()

    if not coupon:
        return CouponApplyResponse(valid=False, message="Invalid coupon code")

    today = date_type.today()
    if today < coupon.valid_from or today > coupon.valid_until:
        return CouponApplyResponse(valid=False, message="Coupon has expired")

    if coupon.used_count >= coupon.max_uses:
        return CouponApplyResponse(valid=False, message="Coupon usage limit reached")

    if data.subtotal < coupon.min_order_value:
        return CouponApplyResponse(
            valid=False,
            message=f"Minimum order value is ₹{coupon.min_order_value}",
        )

    if coupon.discount_type == "flat":
        discount = coupon.discount_value
    else:
        discount = round(data.subtotal * coupon.discount_value / 100, 2)

    return CouponApplyResponse(
        valid=True,
        discount=discount,
        message=f"Coupon applied! You save ₹{discount}",
    )
