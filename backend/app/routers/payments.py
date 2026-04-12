"""Payment router — PayU order initialization, callback handling, and verification."""
from urllib.parse import urlencode

from fastapi import APIRouter, Depends, HTTPException, Request, status
from fastapi.responses import HTMLResponse, RedirectResponse
from sqlalchemy.orm import Session
from pydantic import BaseModel
from typing import Optional, Dict

from app.database import get_db
from app.models.order import Order
from app.models.user import User
from app.services.payment_service import (
    create_payu_payment_payload,
    verify_payu_response_hash,
    fetch_payu_transaction_status,
)
from app.utils.jwt import get_current_user
from app.config import get_settings

import logging

logger = logging.getLogger(__name__)
settings = get_settings()
router = APIRouter()


# --- Schemas ---

class CreatePaymentOrderRequest(BaseModel):
    order_id: str
    platform: Optional[str] = "web"  # web | android


class CreatePaymentOrderResponse(BaseModel):
    provider: str
    mode: str
    txnid: str
    amount: str
    currency: str
    order_id: str
    payment_url: str
    checkout_url: str
    method: str
    fields: Dict[str, str]
    payment_options: list[str]
    upi_intent_supported: bool


class VerifyPaymentRequest(BaseModel):
    order_id: str
    txnid: Optional[str] = None
    status: Optional[str] = None
    mihpayid: Optional[str] = None
    hash: Optional[str] = None
    raw_payload: Optional[Dict[str, str]] = None


class PaymentStatusResponse(BaseModel):
    order_id: str
    payment_status: str
    message: str


# --- Endpoints ---

@router.post("/payments/create-order", response_model=CreatePaymentOrderResponse)
def create_payment_order(
    request: Request,
    data: CreatePaymentOrderRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Create a PayU payment initialization payload for hosted checkout."""
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

    backend_base_url = settings.PAYU_CALLBACK_BASE_URL.strip() or str(request.base_url).rstrip("/")

    payload = create_payu_payment_payload(
        order_id=order.id,
        amount_rupees=float(order.total),
        customer_name=current_user.name or "Customer",
        customer_email=current_user.email or "customer@openbake.in",
        customer_phone=current_user.phone or "9999999999",
        platform=(data.platform or "web").lower(),
        callback_base_url=backend_base_url,
    )

    # Reuse existing txn/payment columns to avoid risky schema migration.
    order.razorpay_order_id = payload["txnid"]
    db.commit()

    return CreatePaymentOrderResponse(
        provider=payload["provider"],
        mode=payload["mode"],
        txnid=payload["txnid"],
        amount=payload["amount"],
        currency="INR",
        order_id=order.id,
        payment_url=payload["payment_url"],
        checkout_url=f"{backend_base_url}/api/payments/payu/hosted?{urlencode(payload['fields'])}",
        method=payload["method"],
        fields=payload["fields"],
        payment_options=payload["payment_options"],
        upi_intent_supported=payload["upi_intent_supported"],
    )


@router.get("/payments/payu/hosted", response_class=HTMLResponse)
def payu_hosted_page(request: Request):
    allowed_keys = {
        "key", "txnid", "amount", "productinfo", "firstname", "email", "phone",
        "surl", "furl", "hash", "service_provider", "udf1", "udf2", "udf3", "udf4", "udf5",
    }
    fields = {k: v for k, v in request.query_params.items() if k in allowed_keys}
    inputs_html = "\n".join(
        [f'<input type="hidden" name="{k}" value="{v}" />' for k, v in fields.items()]
    )
    return HTMLResponse(
        content=(
            "<!doctype html><html><head><meta charset='utf-8'/>"
            "<meta name='viewport' content='width=device-width, initial-scale=1'/>"
            "<title>Redirecting to PayU...</title></head><body>"
            "<p style='font-family:sans-serif;padding:16px'>Redirecting to secure payment...</p>"
            f"<form id='payuForm' method='post' action='{settings.PAYU_PAYMENT_URL}'>{inputs_html}</form>"
            "<script>document.getElementById('payuForm').submit();</script>"
            "</body></html>"
        )
    )


@router.post("/payments/verify", response_model=PaymentStatusResponse)
def verify_payment(
    data: VerifyPaymentRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Verify payment callback payload and update order payment status."""
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

    payload = data.raw_payload or {}
    payload = {
        **payload,
        "txnid": data.txnid or payload.get("txnid") or order.razorpay_order_id,
        "status": data.status or payload.get("status") or order.payment_status,
        "mihpayid": data.mihpayid or payload.get("mihpayid") or "",
        "hash": data.hash or payload.get("hash") or "",
        "udf1": payload.get("udf1") or order.id,
    }

    is_valid = verify_payu_response_hash(payload)
    is_success = str(payload.get("status", "")).lower() == "success"

    if is_valid and is_success:
        order.payment_status = "paid"
        if payload.get("mihpayid"):
            order.razorpay_payment_id = str(payload["mihpayid"])
        db.commit()
        logger.info("payment_verified", extra={"order_id": order.id, "txnid": payload.get("txnid")})
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
            detail="Payment verification failed",
        )


def _build_result_redirect(order_id: str, status_value: str, platform: str) -> str:
    status_value = status_value.lower()
    is_success = status_value == "success"
    if platform == "android":
        base = settings.ANDROID_DEEP_LINK_BASE.rstrip("/")
        return f"{base}?order_id={order_id}&payment_status={'paid' if is_success else 'failed'}"

    base = settings.WEB_BASE_URL.rstrip("/")
    query = urlencode({
        "order_id": order_id,
        "payment_status": "paid" if is_success else "failed",
        "source": "payu",
    })
    return f"{base}/orders?{query}"


@router.api_route("/payments/payu/callback/success", methods=["GET", "POST"])
async def payu_success_callback(request: Request, db: Session = Depends(get_db)):
    form_payload = {}
    if request.method == "POST":
        form = await request.form()
        form_payload = {k: str(v) for k, v in form.items()}
    else:
        form_payload = {k: str(v) for k, v in request.query_params.items()}

    order_id = form_payload.get("udf1", "")
    platform = (form_payload.get("udf2", "web") or "web").lower()
    txnid = form_payload.get("txnid", "")
    status_value = (form_payload.get("status", "") or "").lower()

    if not order_id:
        return RedirectResponse(url=_build_result_redirect("unknown", "failed", platform), status_code=302)

    order = db.query(Order).filter(Order.id == order_id).first()
    if order:
        hash_valid = verify_payu_response_hash(form_payload)
        if hash_valid and status_value == "success":
            order.payment_status = "paid"
            order.razorpay_order_id = txnid or order.razorpay_order_id
            order.razorpay_payment_id = form_payload.get("mihpayid") or order.razorpay_payment_id
            db.commit()
        elif status_value == "success":
            logger.warning(
                "payu_success_hash_mismatch",
                extra={"order_id": order_id, "txnid": txnid},
            )
            order.payment_status = "pending"
            order.razorpay_order_id = txnid or order.razorpay_order_id
            db.commit()
        else:
            order.payment_status = "failed"
            db.commit()

    return RedirectResponse(url=_build_result_redirect(order_id, status_value or "failed", platform), status_code=302)


@router.api_route("/payments/payu/callback/failure", methods=["GET", "POST"])
async def payu_failure_callback(request: Request, db: Session = Depends(get_db)):
    form_payload = {}
    if request.method == "POST":
        form = await request.form()
        form_payload = {k: str(v) for k, v in form.items()}
    else:
        form_payload = {k: str(v) for k, v in request.query_params.items()}

    order_id = form_payload.get("udf1", "")
    platform = (form_payload.get("udf2", "web") or "web").lower()
    txnid = form_payload.get("txnid", "")

    if order_id:
        order = db.query(Order).filter(Order.id == order_id).first()
        if order:
            order.payment_status = "failed"
            order.razorpay_order_id = txnid or order.razorpay_order_id
            db.commit()

    return RedirectResponse(url=_build_result_redirect(order_id or "unknown", "failed", platform), status_code=302)


@router.get("/payments/status/{order_id}", response_model=PaymentStatusResponse)
def payment_status(
    order_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    order = (
        db.query(Order)
        .filter(Order.id == order_id, Order.user_id == current_user.id)
        .first()
    )
    if not order:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Order not found")

    if (
        order.payment_status == "pending"
        and (order.payment_method or "").lower() != "cod"
        and order.razorpay_order_id
    ):
        remote = fetch_payu_transaction_status(order.razorpay_order_id)
        remote_status = (remote.get("status") or "").lower()

        if remote_status == "success":
            order.payment_status = "paid"
            remote_payment_id = remote.get("mihpayid")
            if remote_payment_id:
                order.razorpay_payment_id = remote_payment_id
            db.commit()
            logger.info(
                "payment_status_reconciled_paid",
                extra={"order_id": order.id, "txnid": order.razorpay_order_id},
            )
        elif remote_status in {"failure", "failed", "cancelled", "cancel", "dropped", "bounced"}:
            order.payment_status = "failed"
            db.commit()
            logger.info(
                "payment_status_reconciled_failed",
                extra={"order_id": order.id, "txnid": order.razorpay_order_id},
            )

    return PaymentStatusResponse(
        order_id=order.id,
        payment_status=order.payment_status,
        message="Payment status fetched",
    )
