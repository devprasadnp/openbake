from datetime import datetime, timezone, timedelta
from typing import Optional

IST = timezone(timedelta(hours=5, minutes=30), name="IST")


def now_utc() -> datetime:
    return datetime.now(timezone.utc)


def ensure_aware_utc(value: Optional[datetime]) -> Optional[datetime]:
    if value is None:
        return None
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)


def to_ist(value: Optional[datetime]) -> Optional[datetime]:
    aware = ensure_aware_utc(value)
    if aware is None:
        return None
    return aware.astimezone(IST)


def to_ist_iso(value: Optional[datetime]) -> Optional[str]:
    converted = to_ist(value)
    return converted.isoformat() if converted else None
