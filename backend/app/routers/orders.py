from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError

import asyncio
import json
import time

from app.database import get_db, SessionLocal
from app.models.order import Order
from app.models.user import User
from app.models.coupon import Coupon
from app.schemas.order import (
    OrderCreate, OrderResponse, CartValidateRequest, CartValidateResponse,
    CouponApplyRequest, CouponApplyResponse,
)
from app.services.order_service import place_order, calculate_order_totals, restore_stock_for_order
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
    restore_stock_for_order(db, order)
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


# --- SSE Order Tracking ---

@router.get("/orders/{order_id}/stream")
async def stream_order_status(
    order_id: str,
    current_user: User = Depends(get_current_user),
):
    """Server-Sent Events stream for real-time order status updates.
    
    The client connects and receives events whenever the order status changes.
    The stream closes when the order reaches a terminal state (delivered/cancelled).
    """
    # Validate order ownership first
    db = SessionLocal()
    try:
        order = (
            db.query(Order)
            .filter(Order.id == order_id, Order.user_id == current_user.id)
            .first()
        )
        if not order:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Order not found")
        initial_status = order.status
    finally:
        db.close()

    async def event_generator():
        last_status = initial_status
        terminal_states = {"delivered", "cancelled"}
        
        # Send initial status immediately
        data = json.dumps({"status": last_status, "order_id": order_id})
        yield f"data: {data}\n\n"

        if last_status in terminal_states:
            return

        while True:
            await asyncio.sleep(3)  # Poll every 3 seconds
            db_poll = SessionLocal()
            try:
                order_poll = db_poll.query(Order).filter(Order.id == order_id).first()
                if not order_poll:
                    break
                
                current_status = order_poll.status
                if current_status != last_status:
                    last_status = current_status
                    data = json.dumps({
                        "status": current_status,
                        "order_id": order_id,
                        "estimated_delivery_minutes": order_poll.estimated_delivery_minutes,
                        "payment_status": order_poll.payment_status,
                    })
                    yield f"data: {data}\n\n"

                if current_status in terminal_states:
                    break
            except Exception:
                break
            finally:
                db_poll.close()

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
        },
    )
