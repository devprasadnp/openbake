from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, status
from sqlalchemy.orm import Session
import os
import uuid as uuid_mod

from app.database import get_db
from app.models.user import User, Address
from app.models.review import Review, WishlistItem
from app.models.product import Product
from app.models.order import Order, OrderItem
from app.schemas.auth import UserResponse, ProfileUpdateRequest, AddressCreate, AddressResponse
from app.schemas.order import ReviewCreate, ReviewResponse
from app.utils.jwt import get_current_user

MEDIA_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "media", "avatars")

router = APIRouter()


# --- Profile ---

@router.get("/profile", response_model=UserResponse)
def get_profile(current_user: User = Depends(get_current_user)):
    """Get current user's profile."""
    return current_user


@router.patch("/profile", response_model=UserResponse)
def update_profile(
    data: ProfileUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update current user's profile."""
    if data.name is not None:
        current_user.name = data.name
    if data.phone is not None:
        current_user.phone = data.phone
    if data.profile_image_url is not None:
        current_user.profile_image_url = data.profile_image_url
    db.commit()
    db.refresh(current_user)
    return current_user


@router.post("/profile/avatar", response_model=UserResponse)
def upload_avatar(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Upload a profile avatar image. Accepts JPEG/PNG, max 5MB."""
    if file.content_type not in ("image/jpeg", "image/png", "image/webp"):
        raise HTTPException(status_code=400, detail="Only JPEG, PNG, and WebP images are allowed")

    contents = file.read()
    if len(contents) > 5 * 1024 * 1024:
        raise HTTPException(status_code=400, detail="Image must be smaller than 5MB")

    ext = file.filename.rsplit(".", 1)[-1] if file.filename and "." in file.filename else "jpg"
    filename = f"{current_user.id}_{uuid_mod.uuid4().hex[:8]}.{ext}"
    filepath = os.path.join(MEDIA_DIR, filename)
    os.makedirs(MEDIA_DIR, exist_ok=True)

    # Remove old avatar file if it exists
    if current_user.profile_image_url:
        old_filename = current_user.profile_image_url.rsplit("/", 1)[-1]
        old_path = os.path.join(MEDIA_DIR, old_filename)
        if os.path.exists(old_path):
            os.remove(old_path)

    with open(filepath, "wb") as f:
        f.write(contents)

    current_user.profile_image_url = f"/media/avatars/{filename}"
    db.commit()
    db.refresh(current_user)
    return current_user


# --- Addresses ---

@router.get("/addresses", response_model=list[AddressResponse])
def list_addresses(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """List saved addresses."""
    return db.query(Address).filter(Address.user_id == current_user.id).all()


@router.post("/addresses", response_model=AddressResponse)
def add_address(
    data: AddressCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Add a new address."""
    try:
        address = Address(user_id=current_user.id, **data.model_dump())
        db.add(address)
        db.commit()
        db.refresh(address)
        return address
    except Exception as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Failed to save address: {str(e)}",
        )


@router.patch("/addresses/{address_id}", response_model=AddressResponse)
def update_address(
    address_id: str,
    data: AddressCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update an existing address."""
    address = (
        db.query(Address)
        .filter(Address.id == address_id, Address.user_id == current_user.id)
        .first()
    )
    if not address:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Address not found")
    for field, value in data.model_dump().items():
        setattr(address, field, value)
    db.commit()
    db.refresh(address)
    return address


@router.delete("/addresses/{address_id}")
def delete_address(
    address_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Delete a saved address."""
    address = (
        db.query(Address)
        .filter(Address.id == address_id, Address.user_id == current_user.id)
        .first()
    )
    if not address:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Address not found")
    db.delete(address)
    db.commit()
    return {"message": "Address deleted"}


# --- Reviews ---

@router.post("/reviews", response_model=ReviewResponse)
def submit_review(
    data: ReviewCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Submit a product review (only for delivered orders the user owns)."""
    # Check product exists
    product = db.query(Product).filter(Product.id == data.product_id).first()
    if not product:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Product not found")

    # Verify order ownership and status
    order = (
        db.query(Order)
        .filter(Order.id == data.order_id, Order.user_id == current_user.id)
        .first()
    )
    if not order:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Order not found or does not belong to you",
        )
    if order.status != "delivered":
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Reviews can only be submitted for delivered orders",
        )

    # Verify the product was part of this order
    order_item = (
        db.query(OrderItem)
        .filter(OrderItem.order_id == data.order_id, OrderItem.product_id == data.product_id)
        .first()
    )
    if not order_item:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="This product was not part of the specified order",
        )

    # Prevent duplicate reviews
    existing_review = (
        db.query(Review)
        .filter(
            Review.user_id == current_user.id,
            Review.product_id == data.product_id,
            Review.order_id == data.order_id,
        )
        .first()
    )
    if existing_review:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="You have already reviewed this product for this order",
        )

    review = Review(
        user_id=current_user.id,
        product_id=data.product_id,
        order_id=data.order_id,
        rating=data.rating,
        comment=data.comment,
    )
    db.add(review)
    db.commit()
    db.refresh(review)

    # Update product average rating
    from sqlalchemy import func
    avg_rating = db.query(func.avg(Review.rating)).filter(Review.product_id == data.product_id).scalar()
    if avg_rating:
        product.rating = round(float(avg_rating), 1)
        db.commit()

    return review


# --- Wishlist ---

@router.get("/wishlist")
def get_wishlist(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get wishlist items with product details."""
    items = (
        db.query(WishlistItem)
        .filter(WishlistItem.user_id == current_user.id)
        .all()
    )
    result = []
    for item in items:
        product = db.query(Product).filter(Product.id == item.product_id).first()
        if product:
            result.append({
                "id": str(item.id),
                "product_id": str(item.product_id),
                "product": {
                    "id": str(product.id),
                    "name": product.name,
                    "description": product.description,
                    "price": product.price,
                    "images": product.images,
                    "rating": product.rating,
                    "is_available": product.is_available,
                    "is_eggless_available": product.is_eggless_available,
                    "category_id": str(product.category_id),
                    "customizable": product.customizable,
                    "stock_count": product.stock_count,
                    "variants": [],
                },
            })
    return result


@router.post("/wishlist/{product_id}")
def add_to_wishlist(
    product_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Add a product to wishlist."""
    existing = (
        db.query(WishlistItem)
        .filter(WishlistItem.user_id == current_user.id, WishlistItem.product_id == product_id)
        .first()
    )
    if existing:
        return {"message": "Already in wishlist"}

    item = WishlistItem(user_id=current_user.id, product_id=product_id)
    db.add(item)
    db.commit()
    return {"message": "Added to wishlist"}


@router.delete("/wishlist/{product_id}")
def remove_from_wishlist(
    product_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Remove a product from wishlist."""
    item = (
        db.query(WishlistItem)
        .filter(WishlistItem.user_id == current_user.id, WishlistItem.product_id == product_id)
        .first()
    )
    if not item:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Item not in wishlist")
    db.delete(item)
    db.commit()
    return {"message": "Removed from wishlist"}
