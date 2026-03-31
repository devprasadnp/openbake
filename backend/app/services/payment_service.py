"""Payment service — Razorpay integration for UPI, card, and wallet payments."""
import hashlib
import hmac
import logging

from app.config import get_settings

settings = get_settings()
logger = logging.getLogger(__name__)

# Razorpay client (lazy init)
_razorpay_client = None


def _get_razorpay_client():
    """Lazily initialize the Razorpay client."""
    global _razorpay_client
    if _razorpay_client is None:
        if not settings.RAZORPAY_KEY_ID or not settings.RAZORPAY_KEY_SECRET:
            raise ValueError("Razorpay credentials not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.")
        import razorpay
        _razorpay_client = razorpay.Client(auth=(settings.RAZORPAY_KEY_ID, settings.RAZORPAY_KEY_SECRET))
    return _razorpay_client


def create_razorpay_order(amount_paise: int, receipt: str, notes: dict = None) -> dict:
    """Create a Razorpay order.
    
    Args:
        amount_paise: Amount in paise (e.g., 10000 = ₹100).
        receipt: A unique receipt ID (typically the order ID).
        notes: Optional metadata dict.
    
    Returns:
        Razorpay order dict with 'id', 'amount', 'currency', etc.
    """
    client = _get_razorpay_client()
    order_data = {
        "amount": amount_paise,
        "currency": "INR",
        "receipt": receipt,
        "notes": notes or {},
    }
    try:
        razorpay_order = client.order.create(data=order_data)
        logger.info("razorpay_order_created", extra={"razorpay_order_id": razorpay_order["id"], "receipt": receipt})
        return razorpay_order
    except Exception as e:
        logger.error("razorpay_order_creation_failed", extra={"receipt": receipt, "error": str(e)})
        raise ValueError(f"Failed to create payment order: {str(e)}")


def verify_payment_signature(
    razorpay_order_id: str,
    razorpay_payment_id: str,
    razorpay_signature: str,
) -> bool:
    """Verify Razorpay payment signature using HMAC SHA256.
    
    Returns True if signature is valid, False otherwise.
    """
    message = f"{razorpay_order_id}|{razorpay_payment_id}"
    expected_signature = hmac.new(
        settings.RAZORPAY_KEY_SECRET.encode("utf-8"),
        message.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()
    return hmac.compare_digest(expected_signature, razorpay_signature)
