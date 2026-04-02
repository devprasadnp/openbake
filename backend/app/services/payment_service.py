"""Payment service — Razorpay integration for UPI, card, and wallet payments."""
import hashlib
import hmac
import logging
import uuid

from app.config import get_settings

settings = get_settings()
logger = logging.getLogger(__name__)

# Razorpay client (lazy init)
_razorpay_client = None
_razorpay_available = None  # None = not checked yet


def _get_razorpay_client():
    """Lazily initialize the Razorpay client. Returns None if credentials are invalid."""
    global _razorpay_client, _razorpay_available
    if _razorpay_available is False:
        return None
    if _razorpay_client is not None:
        return _razorpay_client
    
    key_id = settings.RAZORPAY_KEY_ID
    key_secret = settings.RAZORPAY_KEY_SECRET
    
    if not key_id or not key_secret or "placeholder" in key_id or "placeholder" in key_secret:
        logger.warning("Razorpay credentials not configured — using dev mock mode")
        _razorpay_available = False
        return None
    
    try:
        import razorpay
        _razorpay_client = razorpay.Client(auth=(key_id, key_secret))
        _razorpay_available = True
        return _razorpay_client
    except Exception as e:
        logger.warning(f"Failed to init Razorpay client: {e} — using dev mock mode")
        _razorpay_available = False
        return None


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

    # Dev mock mode — simulate a Razorpay order when no valid credentials
    if client is None:
        mock_order_id = f"order_dev_{uuid.uuid4().hex[:16]}"
        logger.info("razorpay_mock_order_created", extra={"razorpay_order_id": mock_order_id, "receipt": receipt})
        return {
            "id": mock_order_id,
            "amount": amount_paise,
            "currency": "INR",
            "receipt": receipt,
            "status": "created",
        }

    try:
        razorpay_order = client.order.create(data=order_data)
        logger.info("razorpay_order_created", extra={"razorpay_order_id": razorpay_order["id"], "receipt": receipt})
        return razorpay_order
    except Exception as e:
        logger.error("razorpay_order_creation_failed", extra={"receipt": receipt, "error": str(e)})
        # Fallback to mock in dev
        if settings.APP_ENV == "development":
            mock_order_id = f"order_dev_{uuid.uuid4().hex[:16]}"
            logger.warning("falling_back_to_mock_order", extra={"razorpay_order_id": mock_order_id})
            return {
                "id": mock_order_id,
                "amount": amount_paise,
                "currency": "INR",
                "receipt": receipt,
                "status": "created",
            }
        raise ValueError(f"Failed to create payment order: {str(e)}")


def verify_payment_signature(
    razorpay_order_id: str,
    razorpay_payment_id: str,
    razorpay_signature: str,
) -> bool:
    """Verify Razorpay payment signature using HMAC SHA256.
    
    Returns True if signature is valid, False otherwise.
    In dev mock mode (order IDs starting with 'order_dev_'), always returns True.
    """
    # Dev mock mode — auto-accept
    if razorpay_order_id.startswith("order_dev_"):
        logger.info("dev_mock_payment_verified", extra={"razorpay_order_id": razorpay_order_id})
        return True

    message = f"{razorpay_order_id}|{razorpay_payment_id}"
    expected_signature = hmac.new(
        settings.RAZORPAY_KEY_SECRET.encode("utf-8"),
        message.encode("utf-8"),
        hashlib.sha256,
    ).hexdigest()
    return hmac.compare_digest(expected_signature, razorpay_signature)
