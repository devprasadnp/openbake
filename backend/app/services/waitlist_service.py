"""Waitlist service — manages waitlist queue and notifications."""
import logging
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.models.waitlist import WaitlistItem as WaitlistItemModel
from app.models.product import Product

logger = logging.getLogger(__name__)


def join_waitlist(db: Session, user_id: str, product_id: str, variant_id: str = None) -> WaitlistItemModel:
    """Add a user to the waitlist for an out-of-stock product."""
    # Check product exists
    product = db.query(Product).filter(Product.id == product_id).first()
    if not product:
        raise ValueError("Product not found")

    # Product must actually be out of stock
    if product.stock_count > 0 and product.is_available:
        raise ValueError("Product is currently in stock — add it to cart instead")

    # Check for existing waitlist entry
    existing = (
        db.query(WaitlistItemModel)
        .filter(
            WaitlistItemModel.user_id == user_id,
            WaitlistItemModel.product_id == product_id,
            WaitlistItemModel.status == "waiting",
        )
        .first()
    )
    if existing:
        raise ValueError("Already on the waitlist for this product")

    item = WaitlistItemModel(
        user_id=user_id,
        product_id=product_id,
        variant_id=variant_id,
        status="waiting",
    )
    db.add(item)
    db.commit()
    db.refresh(item)
    logger.info("waitlist_joined", extra={"user_id": user_id, "product_id": product_id})
    return item


def leave_waitlist(db: Session, user_id: str, product_id: str) -> None:
    """Remove a user from the waitlist."""
    item = (
        db.query(WaitlistItemModel)
        .filter(
            WaitlistItemModel.user_id == user_id,
            WaitlistItemModel.product_id == product_id,
            WaitlistItemModel.status == "waiting",
        )
        .first()
    )
    if not item:
        raise ValueError("Not on the waitlist for this product")
    db.delete(item)
    db.commit()
    logger.info("waitlist_left", extra={"user_id": user_id, "product_id": product_id})


def get_user_waitlist(db: Session, user_id: str) -> list:
    """Get all waitlist items for a user."""
    return (
        db.query(WaitlistItemModel)
        .filter(WaitlistItemModel.user_id == user_id)
        .order_by(WaitlistItemModel.created_at.desc())
        .all()
    )


def notify_waitlist_users(db: Session, product_id: str) -> int:
    """Notify all waiting users that a product is back in stock.
    
    Called when admin restocks a product (stock goes from 0 to > 0).
    Returns the number of users notified.
    """
    waiting_items = (
        db.query(WaitlistItemModel)
        .filter(
            WaitlistItemModel.product_id == product_id,
            WaitlistItemModel.status == "waiting",
        )
        .order_by(WaitlistItemModel.created_at.asc())  # FIFO
        .all()
    )

    notified_count = 0
    for item in waiting_items:
        item.status = "notified"
        item.notified_at = datetime.now(timezone.utc)
        notified_count += 1
        # TODO: Send push notification via FCM when device tokens are available
        logger.info(
            "waitlist_user_notified",
            extra={"user_id": item.user_id, "product_id": product_id},
        )

    if notified_count > 0:
        db.commit()
        logger.info(
            "waitlist_notifications_sent",
            extra={"product_id": product_id, "count": notified_count},
        )

    return notified_count


def get_admin_waitlist(db: Session, product_id: str = None) -> list:
    """Admin view: get all waitlist entries, optionally filtered by product."""
    query = db.query(WaitlistItemModel).order_by(WaitlistItemModel.created_at.desc())
    if product_id:
        query = query.filter(WaitlistItemModel.product_id == product_id)
    return query.all()
