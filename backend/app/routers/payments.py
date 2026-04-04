"""Payment router — Razorpay order creation, verification, and webhooks."""
from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import Optional

from app.database import get_db
from app.models.order import Order
from app.models.user import User
from app.services.payment_service import create_razorpay_order, verify_payment_signature
from app.utils.jwt import get_current_user
from app.config import get_settings

import logging

logger = logging.getLogger(__name__)
settings = get_settings()
router = APIRouter()


# --- Schemas ---

class CreatePaymentOrderRequest(BaseModel):
    order_id: str


class CreatePaymentOrderResponse(BaseModel):
    razorpay_order_id: str
    razorpay_key_id: str
    amount: int  # paise
    currency: str
    order_id: str


class VerifyPaymentRequest(BaseModel):
    order_id: str
    razorpay_order_id: str
    razorpay_payment_id: str
    razorpay_signature: str


class PaymentStatusResponse(BaseModel):
    order_id: str
    payment_status: str
    message: str


# --- Endpoints ---

@router.post("/payments/create-order", response_model=CreatePaymentOrderResponse)
def create_payment_order(
    data: CreatePaymentOrderRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Create a Razorpay order for payment processing."""
    order = (
        db.query(Order)
        .filter(Order.id == data.order_id, Order.user_id == current_user.id)
        .first()
    )
    if not order:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Order not found")

    if order.payment_status == "paid":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Order is already paid",
        )

    if order.payment_method == "cod":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="COD orders do not require online payment",
        )

    # Amount in paise (Razorpay expects integer paise)
    amount_paise = int(round(order.total * 100))

    try:
        razorpay_order = create_razorpay_order(
            amount_paise=amount_paise,
            receipt=order.id,
            notes={"user_id": current_user.id, "order_type": order.order_type},
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=str(e),
        )

    # Store razorpay_order_id on the order
    order.razorpay_order_id = razorpay_order["id"]
    db.commit()

    return CreatePaymentOrderResponse(
        razorpay_order_id=razorpay_order["id"],
        razorpay_key_id=settings.RAZORPAY_KEY_ID,
        amount=amount_paise,
        currency="INR",
        order_id=order.id,
    )


@router.post("/payments/verify", response_model=PaymentStatusResponse)
def verify_payment(
    data: VerifyPaymentRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Verify Razorpay payment signature and update order payment status."""
    order = (
        db.query(Order)
        .filter(Order.id == data.order_id, Order.user_id == current_user.id)
        .first()
    )
    if not order:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Order not found")

    if order.payment_status == "paid":
        return PaymentStatusResponse(
            order_id=order.id,
            payment_status="paid",
            message="Payment already verified",
        )

    # Verify signature
    is_valid = verify_payment_signature(
        razorpay_order_id=data.razorpay_order_id,
        razorpay_payment_id=data.razorpay_payment_id,
        razorpay_signature=data.razorpay_signature,
    )

    if is_valid:
        order.payment_status = "paid"
        order.razorpay_payment_id = data.razorpay_payment_id
        db.commit()
        logger.info("payment_verified", extra={"order_id": order.id, "payment_id": data.razorpay_payment_id})
        return PaymentStatusResponse(
            order_id=order.id,
            payment_status="paid",
            message="Payment verified successfully",
        )
    else:
        order.payment_status = "failed"
        db.commit()
        logger.warning("payment_verification_failed", extra={"order_id": order.id})
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Payment verification failed — invalid signature",
        )


@router.post("/payments/webhook")
async def razorpay_webhook(request: Request, db: Session = Depends(get_db)):
    """Razorpay webhook for async payment events (payment.captured, payment.failed)."""
    import hashlib
    import hmac as hmac_mod
    import json

    body = await request.body()

    # Verify webhook signature
    webhook_secret = settings.RAZORPAY_KEY_SECRET
    received_signature = request.headers.get("X-Razorpay-Signature", "")

    expected_sig = hmac_mod.new(
        webhook_secret.encode("utf-8"),
        body,
        hashlib.sha256,
    ).hexdigest()

    if not hmac_mod.compare_digest(expected_sig, received_signature):
        logger.warning("webhook_invalid_signature")
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid webhook signature")

    payload = json.loads(body)
    event = payload.get("event", "")

    if event == "payment.captured":
        payment = payload.get("payload", {}).get("payment", {}).get("entity", {})
        receipt = payment.get("notes", {}).get("receipt") or payment.get("description", "")
        razorpay_payment_id = payment.get("id")

        # Try to find order by razorpay_order_id
        razorpay_order_id = payment.get("order_id")
        if razorpay_order_id:
            order = db.query(Order).filter(Order.razorpay_order_id == razorpay_order_id).first()
            if order and order.payment_status != "paid":
                order.payment_status = "paid"
                order.razorpay_payment_id = razorpay_payment_id
                db.commit()
                logger.info("webhook_payment_captured", extra={"order_id": order.id})

    elif event == "payment.failed":
        payment = payload.get("payload", {}).get("payment", {}).get("entity", {})
        razorpay_order_id = payment.get("order_id")
        if razorpay_order_id:
            order = db.query(Order).filter(Order.razorpay_order_id == razorpay_order_id).first()
            if order and order.payment_status == "pending":
                order.payment_status = "failed"
                # Restore stock for failed payment orders
                from app.services.order_service import restore_stock_for_order
                order.status = "cancelled"
                restore_stock_for_order(db, order)
                db.commit()
                logger.info("webhook_payment_failed_order_cancelled", extra={"order_id": order.id})

    return {"status": "ok"}
