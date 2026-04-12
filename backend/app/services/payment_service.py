"""Payment service — PayU hosted checkout helpers with signature verification."""
import hashlib
import logging
import uuid
import httpx

from app.config import get_settings

settings = get_settings()
logger = logging.getLogger(__name__)


def _normalized_amount(amount_rupees: float) -> str:
    return f"{amount_rupees:.2f}"


def _normalize_payu_field(value: object) -> str:
    normalized = str(value or "").strip()
    if normalized.lower() in {"no value", "none", "null", "nil"}:
        return ""
    return normalized


def _payu_hash_string(
    key: str,
    txnid: str,
    amount: str,
    productinfo: str,
    firstname: str,
    email: str,
    udf1: str,
    udf2: str,
    udf3: str,
    udf4: str,
    udf5: str,
    salt: str,
) -> str:
    raw = (
        f"{key}|{txnid}|{amount}|{productinfo}|{firstname}|{email}|"
        f"{udf1}|{udf2}|{udf3}|{udf4}|{udf5}||||||{salt}"
    )
    return hashlib.sha512(raw.encode("utf-8")).hexdigest().lower()


def create_payu_payment_payload(
    *,
    order_id: str,
    amount_rupees: float,
    customer_name: str,
    customer_email: str,
    customer_phone: str,
    platform: str,
    callback_base_url: str,
) -> dict:
    """Create merchant-hosted PayU payload for redirection/auto-submit form."""
    key = settings.PAYU_MERCHANT_KEY
    salt = settings.PAYU_MERCHANT_SALT

    txnid = f"txn_{order_id.replace('-', '')[:20]}_{uuid.uuid4().hex[:8]}"
    amount = _normalized_amount(amount_rupees)
    productinfo = f"OpenBake Order {order_id[-8:]}"
    firstname = (customer_name or "Customer").strip()[:60]
    email = (customer_email or "customer@openbake.in").strip()
    phone = (customer_phone or "9999999999").strip()

    udf1 = order_id
    udf2 = platform
    udf3 = ""
    udf4 = ""
    udf5 = ""

    # Dev mock mode when credentials are not available
    if not key or not salt:
        logger.warning("payu_credentials_missing_dev_mock", extra={"order_id": order_id})
        return {
            "provider": "payu",
            "mode": "mock",
            "txnid": f"txn_dev_{uuid.uuid4().hex[:16]}",
            "amount": amount,
            "currency": "INR",
            "payment_url": "",
            "method": "POST",
            "fields": {},
            "payment_options": ["upi_intent", "upi_collect", "cards", "netbanking", "wallets"],
            "upi_intent_supported": True,
        }

    hash_value = _payu_hash_string(
        key=key,
        txnid=txnid,
        amount=amount,
        productinfo=productinfo,
        firstname=firstname,
        email=email,
        udf1=udf1,
        udf2=udf2,
        udf3=udf3,
        udf4=udf4,
        udf5=udf5,
        salt=salt,
    )

    # PayU callbacks go to backend, backend then redirects to web/app result URL.
    callback_base = callback_base_url.rstrip("/")
    surl = f"{callback_base}/api/payments/payu/callback/success"
    furl = f"{callback_base}/api/payments/payu/callback/failure"

    fields = {
        "key": key,
        "txnid": txnid,
        "amount": amount,
        "productinfo": productinfo,
        "firstname": firstname,
        "email": email,
        "phone": phone,
        "surl": surl,
        "furl": furl,
        "hash": hash_value,
        "service_provider": "payu_paisa",
        "udf1": udf1,
        "udf2": udf2,
        "udf3": udf3,
        "udf4": udf4,
        "udf5": udf5,
    }

    return {
        "provider": "payu",
        "mode": "live",
        "txnid": txnid,
        "amount": amount,
        "currency": "INR",
        "payment_url": settings.PAYU_PAYMENT_URL,
        "method": "POST",
        "fields": fields,
        "payment_options": ["upi_intent", "upi_collect", "cards", "netbanking", "wallets"],
        "upi_intent_supported": True,
    }


def verify_payu_response_hash(payload: dict) -> bool:
    """Verify reverse hash from PayU callback response."""
    salt = settings.PAYU_MERCHANT_SALT
    key = settings.PAYU_MERCHANT_KEY

    if not salt or not key:
        # Dev-mode compatibility
        return True

    status = _normalize_payu_field(payload.get("status", ""))
    txnid = _normalize_payu_field(payload.get("txnid", ""))
    amount = _normalize_payu_field(payload.get("amount", ""))
    productinfo = _normalize_payu_field(payload.get("productinfo", ""))
    firstname = _normalize_payu_field(payload.get("firstname", ""))
    email = _normalize_payu_field(payload.get("email", ""))
    udf1 = _normalize_payu_field(payload.get("udf1", ""))
    udf2 = _normalize_payu_field(payload.get("udf2", ""))
    udf3 = _normalize_payu_field(payload.get("udf3", ""))
    udf4 = _normalize_payu_field(payload.get("udf4", ""))
    udf5 = _normalize_payu_field(payload.get("udf5", ""))
    received_hash = _normalize_payu_field(payload.get("hash", "")).lower()
    additional_charges = _normalize_payu_field(payload.get("additionalCharges") or payload.get("additionalcharges"))

    reverse_base = (
        f"{salt}|{status}||||||{udf5}|{udf4}|{udf3}|{udf2}|{udf1}|"
        f"{email}|{firstname}|{productinfo}|{amount}|{txnid}|{key}"
    )
    if additional_charges:
        reverse_base = f"{additional_charges}|{reverse_base}"

    expected_hash = hashlib.sha512(reverse_base.encode("utf-8")).hexdigest().lower()
    return expected_hash == received_hash


def fetch_payu_transaction_status(txnid: str) -> dict:
    """Fetch transaction status from PayU verify_payment API."""
    key = settings.PAYU_MERCHANT_KEY
    salt = settings.PAYU_MERCHANT_SALT

    if not key or not salt or not txnid:
        return {"status": "", "mihpayid": "", "raw": {}}

    command = "verify_payment"
    hash_input = f"{key}|{command}|{txnid}|{salt}"
    request_hash = hashlib.sha512(hash_input.encode("utf-8")).hexdigest().lower()

    payload = {
        "key": key,
        "command": command,
        "var1": txnid,
        "hash": request_hash,
    }

    try:
        response = httpx.post(
            settings.PAYU_STATUS_API_URL,
            data=payload,
            timeout=12.0,
        )
        response.raise_for_status()
        body = response.json()
    except Exception as exc:
        logger.warning("payu_status_fetch_failed", extra={"txnid": txnid, "error": str(exc)})
        return {"status": "", "mihpayid": "", "raw": {}}

    details = body.get("transaction_details") if isinstance(body, dict) else {}
    txn_details = details.get(txnid) if isinstance(details, dict) else {}
    status_value = _normalize_payu_field(txn_details.get("status", "")).lower()
    mihpayid = _normalize_payu_field(txn_details.get("mihpayid", ""))

    return {
        "status": status_value,
        "mihpayid": mihpayid,
        "raw": txn_details if isinstance(txn_details, dict) else {},
    }
