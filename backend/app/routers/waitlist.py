"""Waitlist router — join/leave waitlist, user list, admin view."""
from fastapi import APIRouter, Depends, HTTPException, Query, status
from pydantic import BaseModel
from sqlalchemy.orm import Session
from typing import Optional
from datetime import datetime

from app.database import get_db
from app.models.user import User
from app.models.product import Product
from app.services.waitlist_service import (
    join_waitlist,
    leave_waitlist,
    get_user_waitlist,
    get_admin_waitlist,
)
from app.utils.jwt import get_current_user, require_admin

router = APIRouter()


# --- Schemas ---

class WaitlistItemResponse(BaseModel):
    id: str
    user_id: str
    product_id: str
    product_name: Optional[str] = None
    variant_id: Optional[str] = None
    status: str
    notified_at: Optional[datetime] = None
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class JoinWaitlistRequest(BaseModel):
    variant_id: Optional[str] = None


# --- Endpoints ---

@router.post("/waitlist/{product_id}", response_model=WaitlistItemResponse)
def join_product_waitlist(
    product_id: str,
    data: JoinWaitlistRequest = JoinWaitlistRequest(),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Join the waitlist for an out-of-stock product."""
    try:
        item = join_waitlist(db, current_user.id, product_id, data.variant_id)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))

    product = db.query(Product).filter(Product.id == product_id).first()
    return WaitlistItemResponse(
        id=item.id,
        user_id=item.user_id,
        product_id=item.product_id,
        product_name=product.name if product else None,
        variant_id=item.variant_id,
        status=item.status,
        notified_at=item.notified_at,
        created_at=item.created_at,
    )


@router.delete("/waitlist/{product_id}")
def leave_product_waitlist(
    product_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Leave the waitlist for a product."""
    try:
        leave_waitlist(db, current_user.id, product_id)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
    return {"message": "Removed from waitlist"}


@router.get("/waitlist")
def get_my_waitlist(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get current user's waitlist items with product details."""
    items = get_user_waitlist(db, current_user.id)
    result = []
    for item in items:
        product = db.query(Product).filter(Product.id == item.product_id).first()
        result.append({
            "id": item.id,
            "product_id": item.product_id,
            "product_name": product.name if product else None,
            "product_image": product.images[0] if product and product.images else None,
            "product_price": product.price if product else None,
            "variant_id": item.variant_id,
            "status": item.status,
            "notified_at": item.notified_at,
            "created_at": item.created_at,
        })
    return result


@router.get("/admin/waitlist")
def admin_get_waitlist(
    product_id: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Admin view: all waitlist entries, optionally filtered by product."""
    items = get_admin_waitlist(db, product_id)
    result = []
    for item in items:
        product = db.query(Product).filter(Product.id == item.product_id).first()
        result.append({
            "id": item.id,
            "user_id": item.user_id,
            "product_id": item.product_id,
            "product_name": product.name if product else None,
            "variant_id": item.variant_id,
            "status": item.status,
            "notified_at": item.notified_at,
            "created_at": item.created_at,
        })
    return result
