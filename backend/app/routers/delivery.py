"""Delivery router — delivery fee estimation and config management."""
from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel
from sqlalchemy.orm import Session
from typing import Optional

from app.database import get_db
from app.models.user import User
from app.services.delivery_service import calculate_delivery_fee, get_delivery_config, update_delivery_config
from app.utils.jwt import get_current_user, require_admin

router = APIRouter()


# --- Schemas ---

class DeliveryEstimateResponse(BaseModel):
    distance_km: float
    delivery_fee: float
    estimated_time_minutes: int
    is_free_delivery: bool
    is_deliverable: bool


class DeliveryConfigResponse(BaseModel):
    bakery_lat: float
    bakery_lng: float
    free_delivery_radius_km: float
    delivery_fee_default: float
    speed_min_per_km: float
    cod_enabled: bool = True


class DeliveryConfigUpdate(BaseModel):
    bakery_lat: Optional[float] = None
    bakery_lng: Optional[float] = None
    free_delivery_radius_km: Optional[float] = None
    delivery_fee_default: Optional[float] = None
    speed_min_per_km: Optional[float] = None
    cod_enabled: Optional[bool] = None


# --- Endpoints ---

@router.get("/delivery/estimate", response_model=DeliveryEstimateResponse)
def estimate_delivery(
    lat: float = Query(..., description="Customer latitude"),
    lng: float = Query(..., description="Customer longitude"),
    db: Session = Depends(get_db),
):
    """Calculate delivery fee and ETA based on customer location.
    
    Public endpoint — no auth required (used during checkout).
    """
    if not (-90 <= lat <= 90) or not (-180 <= lng <= 180):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Invalid coordinates",
        )

    result = calculate_delivery_fee(db, lat, lng)
    return DeliveryEstimateResponse(**result)


@router.get("/admin/delivery-config", response_model=DeliveryConfigResponse)
def get_admin_delivery_config(
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Get current delivery configuration."""
    config = get_delivery_config(db)
    return DeliveryConfigResponse(**config)


@router.patch("/admin/delivery-config", response_model=DeliveryConfigResponse)
def update_admin_delivery_config(
    data: DeliveryConfigUpdate,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Update delivery configuration (bakery location, delivery fee, radius, etc.)."""
    updates = data.model_dump(exclude_unset=True)
    if not updates:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="No fields to update",
        )
    config = update_delivery_config(db, updates)
    return DeliveryConfigResponse(**config)
