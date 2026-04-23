"""Delivery service — distance calculation, delivery fee, and ETA estimation."""
import math
from sqlalchemy.orm import Session

from app.config import get_settings
from app.models.settings import AppSettings

settings = get_settings()

# Default bakery location: Bangalore
DEFAULT_BAKERY_LAT = 12.9716
DEFAULT_BAKERY_LNG = 77.5946
DEFAULT_FREE_DELIVERY_RADIUS_KM = 5.0
DEFAULT_DELIVERY_FEE = 40.0
DEFAULT_SPEED_MIN_PER_KM = 3.0  # ~20 km/h average in city


def _get_setting(db: Session, key: str, default: float) -> float:
    """Read a setting from DB, fall back to default."""
    row = db.query(AppSettings).filter(AppSettings.key == key).first()
    if row:
        try:
            return float(row.value)
        except (ValueError, TypeError):
            pass
    return default


def _get_bool_setting(db: Session, key: str, default: bool) -> bool:
    """Read a boolean setting from DB, fall back to default."""
    row = db.query(AppSettings).filter(AppSettings.key == key).first()
    if row:
        return row.value.lower() in ("true", "1", "yes")
    return default


def get_bakery_location(db: Session) -> tuple[float, float]:
    """Return (lat, lng) of the bakery."""
    lat = _get_setting(db, "bakery_lat", DEFAULT_BAKERY_LAT)
    lng = _get_setting(db, "bakery_lng", DEFAULT_BAKERY_LNG)
    return lat, lng


def get_delivery_config(db: Session) -> dict:
    """Return all delivery-related settings."""
    return {
        "bakery_lat": _get_setting(db, "bakery_lat", DEFAULT_BAKERY_LAT),
        "bakery_lng": _get_setting(db, "bakery_lng", DEFAULT_BAKERY_LNG),
        "free_delivery_radius_km": _get_setting(db, "free_delivery_radius_km", DEFAULT_FREE_DELIVERY_RADIUS_KM),
        "delivery_fee_default": _get_setting(db, "delivery_fee_default", DEFAULT_DELIVERY_FEE),
        "speed_min_per_km": _get_setting(db, "speed_min_per_km", DEFAULT_SPEED_MIN_PER_KM),
        "cod_enabled": _get_bool_setting(db, "cod_enabled", True),
    }


def update_delivery_config(db: Session, updates: dict) -> dict:
    """Update delivery settings in DB. Returns the new config."""
    allowed_keys = {"bakery_lat", "bakery_lng", "free_delivery_radius_km", "delivery_fee_default", "speed_min_per_km", "cod_enabled"}
    for key, value in updates.items():
        if key not in allowed_keys:
            continue
        str_value = str(value).lower() if isinstance(value, bool) else str(value)
        row = db.query(AppSettings).filter(AppSettings.key == key).first()
        if row:
            row.value = str_value
        else:
            db.add(AppSettings(key=key, value=str_value))
    db.commit()
    return get_delivery_config(db)


def haversine(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    """Calculate the great-circle distance between two points in km."""
    R = 6371.0  # Earth radius in km
    dlat = math.radians(lat2 - lat1)
    dlng = math.radians(lng2 - lng1)
    a = (
        math.sin(dlat / 2) ** 2
        + math.cos(math.radians(lat1))
        * math.cos(math.radians(lat2))
        * math.sin(dlng / 2) ** 2
    )
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return R * c


def calculate_delivery_fee(
    db: Session,
    customer_lat: float,
    customer_lng: float,
) -> dict:
    """Calculate delivery fee and ETA based on customer location.

    Returns:
        {
            "distance_km": float,
            "delivery_fee": float,
            "estimated_time_minutes": int,
            "is_free_delivery": bool,
            "is_deliverable": bool,
        }
    """
    config = get_delivery_config(db)
    bakery_lat = config["bakery_lat"]
    bakery_lng = config["bakery_lng"]
    free_radius = config["free_delivery_radius_km"]
    default_fee = config["delivery_fee_default"]
    speed = config["speed_min_per_km"]

    distance = haversine(bakery_lat, bakery_lng, customer_lat, customer_lng)
    distance_km = round(distance, 2)

    # Prep time (packing, etc.) + travel time
    prep_time = 15  # minutes
    # When speed is 0 or not set, skip ETA calculation
    if speed > 0:
        travel_time = math.ceil(distance_km * speed)
        estimated_time = prep_time + travel_time
    else:
        estimated_time = None  # Speed estimation disabled

    is_free = distance_km <= free_radius
    delivery_fee = 0.0 if is_free else default_fee

    # Consider orders beyond 25km as undeliverable
    is_deliverable = distance_km <= 25.0

    return {
        "distance_km": distance_km,
        "delivery_fee": delivery_fee,
        "estimated_time_minutes": estimated_time,
        "is_free_delivery": is_free,
        "is_deliverable": is_deliverable,
    }
