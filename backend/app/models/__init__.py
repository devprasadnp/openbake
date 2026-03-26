# Import all models so Base.metadata.create_all() picks them up
from app.models.user import User, Address  # noqa: F401
from app.models.product import Category, Product, ProductVariant  # noqa: F401
from app.models.order import Order, OrderItem  # noqa: F401
from app.models.coupon import Coupon  # noqa: F401
from app.models.review import Review, WishlistItem  # noqa: F401
