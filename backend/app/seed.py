"""
Seed script — populates the database with categories, products, variants, and an admin user.
Run: cd backend && python -m app.seed
"""
import json
from app.database import SessionLocal, engine, Base
from app.models.user import User, Address
from app.models.product import Category, Product, ProductVariant
from app.models.coupon import Coupon
from app.services.auth_service import hash_password
from app.config import get_settings
from datetime import date, timedelta

settings = get_settings()


def seed():
    # Create all tables
    Base.metadata.create_all(bind=engine)

    db = SessionLocal()

    # Check if already seeded based on Products/Categories, not Users (as users might register first)
    if db.query(Category).first() and db.query(Product).first():
        msg = "Database already seeded with products and categories. Skipping."
        print(msg)
        db.close()
        return msg

    print("Seeding database...")

    # ── Admin User ──
    admin = db.query(User).filter_by(email=settings.ADMIN_EMAIL).first()
    if not admin:
        admin = User(
            name="Admin",
            email=settings.ADMIN_EMAIL,
            password_hash=hash_password(settings.ADMIN_PASSWORD),
            auth_provider="email",
            role="admin",
        )
        db.add(admin)
        db.flush()

    # ── Test Customer ──
    customer = db.query(User).filter_by(email="customer@openbake.com").first()
    if not customer:
        customer = User(
            name="Test Customer",
            email="customer@openbake.com",
            phone="9876543210",
            password_hash=hash_password("Customer@123"),
            auth_provider="email",
            role="customer",
        )
        db.add(customer)
        db.flush()

        # ── Customer Address ──
        addr = Address(
            user_id=customer.id,
            label="Home",
            full_address="123, Bakery Lane, Banjara Hills",
            city="Hyderabad",
            pincode="500034",
            is_default=True,
        )
        db.add(addr)
        db.flush()
    db.flush()

    # ── Categories ──
    categories_data = [
        {"name": "Cakes", "image_url": "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=400"},
        {"name": "Pastries", "image_url": "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=400"},
        {"name": "Breads", "image_url": "https://images.unsplash.com/photo-1549931319-a545753d62ce?w=400"},
        {"name": "Cookies", "image_url": "https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=400"},
        {"name": "Cupcakes", "image_url": "https://images.unsplash.com/photo-1587668178277-295251f900ce?w=400"},
        {"name": "Beverages", "image_url": "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=400"},
    ]
    cats = {}
    for c in categories_data:
        cat = db.query(Category).filter_by(name=c["name"]).first()
        if not cat:
            cat = Category(**c)
            db.add(cat)
            db.flush()
        cats[c["name"]] = cat

    # ── Products ──
    products_data = [
        # Cakes
        {
            "category": "Cakes", "name": "Classic Chocolate Cake",
            "description": "Rich, moist chocolate cake layered with velvety chocolate ganache. Perfect for celebrations or as a sweet treat any day.",
            "price": 599, "stock_count": 25, "is_eggless_available": True, "customizable": True, "rating": 4.7,
            "images": ["https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=600"],
            "variants": [
                {"variant_type": "size", "value": "0.5 kg", "extra_price": 0},
                {"variant_type": "size", "value": "1 kg", "extra_price": 350},
                {"variant_type": "size", "value": "2 kg", "extra_price": 800},
                {"variant_type": "flavor", "value": "Dark Chocolate", "extra_price": 0},
                {"variant_type": "flavor", "value": "Truffle", "extra_price": 150},
            ],
        },
        {
            "category": "Cakes", "name": "Red Velvet Cake",
            "description": "Stunning red velvet layers with cream cheese frosting. A showstopper for special occasions.",
            "price": 699, "stock_count": 15, "is_eggless_available": True, "customizable": True, "rating": 4.8,
            "images": ["https://images.unsplash.com/photo-1616541823729-00fe0aacd32c?w=600"],
            "variants": [
                {"variant_type": "size", "value": "0.5 kg", "extra_price": 0},
                {"variant_type": "size", "value": "1 kg", "extra_price": 400},
            ],
        },
        {
            "category": "Cakes", "name": "Butterscotch Crunch Cake",
            "description": "Soft butterscotch sponge loaded with crunchy caramel bits and butterscotch sauce.",
            "price": 549, "stock_count": 20, "is_eggless_available": True, "customizable": True, "rating": 4.5,
            "images": ["https://images.unsplash.com/photo-1621303837174-89787a7d4729?w=600"],
            "variants": [
                {"variant_type": "size", "value": "0.5 kg", "extra_price": 0},
                {"variant_type": "size", "value": "1 kg", "extra_price": 300},
            ],
        },
        {
            "category": "Cakes", "name": "Mango Delight Cake",
            "description": "Fresh mango cream cake made with real Alphonso mangoes. A seasonal favorite.",
            "price": 749, "stock_count": 10, "is_eggless_available": True, "customizable": True, "rating": 4.9,
            "images": ["https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=600"],
            "variants": [
                {"variant_type": "size", "value": "0.5 kg", "extra_price": 0},
                {"variant_type": "size", "value": "1 kg", "extra_price": 500},
            ],
        },
        # Pastries
        {
            "category": "Pastries", "name": "Chocolate Éclair",
            "description": "Choux pastry filled with silky pastry cream and topped with chocolate glaze.",
            "price": 120, "stock_count": 50, "is_eggless_available": False, "customizable": False, "rating": 4.3,
            "images": ["https://images.unsplash.com/photo-1509440159596-0249088772ff?w=600"],
        },
        {
            "category": "Pastries", "name": "Fruit Danish",
            "description": "Flaky puff pastry with custard cream and seasonal fresh fruits.",
            "price": 150, "stock_count": 40, "is_eggless_available": False, "customizable": False, "rating": 4.4,
            "images": ["https://images.unsplash.com/photo-1517433670267-08bbd4be890f?w=600"],
        },
        {
            "category": "Pastries", "name": "Almond Croissant",
            "description": "Buttery croissant filled with almond frangipane and topped with sliced almonds.",
            "price": 180, "stock_count": 35, "is_eggless_available": False, "customizable": False, "rating": 4.6,
            "images": ["https://images.unsplash.com/photo-1530610476181-d83430b64dcd?w=600"],
        },
        # Breads
        {
            "category": "Breads", "name": "Sourdough Loaf",
            "description": "Artisan sourdough bread with a crispy crust and tangy, chewy interior. 72-hour ferment.",
            "price": 250, "stock_count": 30, "is_eggless_available": True, "customizable": False, "rating": 4.8,
            "images": ["https://images.unsplash.com/photo-1589367920969-ab8e050bbb04?w=600"],
        },
        {
            "category": "Breads", "name": "Garlic Focaccia",
            "description": "Italian-style flatbread infused with roasted garlic, olive oil, and rosemary.",
            "price": 199, "stock_count": 25, "is_eggless_available": True, "customizable": False, "rating": 4.5,
            "images": ["https://images.unsplash.com/photo-1586444248902-2367d1a49ef3?w=600"],
        },
        {
            "category": "Breads", "name": "Multigrain Bread",
            "description": "Healthy multigrain bread packed with oats, flax, sunflower seeds, and whole wheat.",
            "price": 149, "stock_count": 40, "is_eggless_available": True, "customizable": False, "rating": 4.2,
            "images": ["https://images.unsplash.com/photo-1509440159596-0249088772ff?w=600"],
        },
        # Cookies
        {
            "category": "Cookies", "name": "Chocolate Chip Cookies (6 pcs)",
            "description": "Classic chewy cookies loaded with premium dark chocolate chips.",
            "price": 199, "stock_count": 60, "is_eggless_available": True, "customizable": False, "rating": 4.6,
            "images": ["https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=600"],
        },
        {
            "category": "Cookies", "name": "Almond Biscotti (8 pcs)",
            "description": "Crunchy Italian-style biscotti with toasted almonds. Perfect with coffee.",
            "price": 249, "stock_count": 45, "is_eggless_available": True, "customizable": False, "rating": 4.4,
            "images": ["https://images.unsplash.com/photo-1590080875515-8a3a8dc5735e?w=600"],
        },
        # Cupcakes
        {
            "category": "Cupcakes", "name": "Vanilla Rainbow Cupcakes (4 pcs)",
            "description": "Fluffy vanilla cupcakes with rainbow buttercream swirl and sprinkles.",
            "price": 349, "stock_count": 30, "is_eggless_available": True, "customizable": True, "rating": 4.7,
            "images": ["https://images.unsplash.com/photo-1587668178277-295251f900ce?w=600"],
            "variants": [
                {"variant_type": "flavor", "value": "Vanilla", "extra_price": 0},
                {"variant_type": "flavor", "value": "Chocolate", "extra_price": 50},
                {"variant_type": "flavor", "value": "Red Velvet", "extra_price": 80},
            ],
        },
        {
            "category": "Cupcakes", "name": "Salted Caramel Cupcakes (4 pcs)",
            "description": "Moist caramel cupcakes with salted caramel buttercream and a drizzle of caramel sauce.",
            "price": 399, "stock_count": 20, "is_eggless_available": True, "customizable": False, "rating": 4.8,
            "images": ["https://images.unsplash.com/photo-1563729784474-d77dbb933a9e?w=600"],
        },
        # Beverages
        {
            "category": "Beverages", "name": "Hot Chocolate",
            "description": "Rich and creamy Belgian hot chocolate topped with whipped cream.",
            "price": 199, "stock_count": 100, "is_eggless_available": True, "customizable": False, "rating": 4.5,
            "images": ["https://images.unsplash.com/photo-1542990253-0d0f5be5f0ed?w=600"],
        },
        {
            "category": "Beverages", "name": "Iced Matcha Latte",
            "description": "Premium Japanese matcha whisked with chilled milk. Smooth and refreshing.",
            "price": 249, "stock_count": 80, "is_eggless_available": True, "customizable": False, "rating": 4.3,
            "images": ["https://images.unsplash.com/photo-1515823064-d6e0c04616a7?w=600"],
        },
    ]

    for pdata in products_data:
        variants = pdata.pop("variants", [])
        cat_name = pdata.pop("category")
        images = pdata.pop("images", [])

        product = db.query(Product).filter_by(name=pdata["name"]).first()
        if not product:
            product = Product(
                category_id=cats[cat_name].id,
                **pdata,
            )
            product.images = images
            db.add(product)
            db.flush()

            for v in variants:
                db.add(ProductVariant(product_id=product.id, **v))

    # ── Coupons ──
    today = date.today()
    coupons = [
        Coupon(code="WELCOME10", discount_type="percent", discount_value=10, min_order_value=200,
               valid_from=today, valid_until=today + timedelta(days=90)),
        Coupon(code="FLAT50", discount_type="flat", discount_value=50, min_order_value=300,
               valid_from=today, valid_until=today + timedelta(days=60)),
        Coupon(code="BAKE20", discount_type="percent", discount_value=20, min_order_value=500,
               max_uses=50, valid_from=today, valid_until=today + timedelta(days=30)),
    ]
    for c in coupons:
        if not db.query(Coupon).filter_by(code=c.code).first():
            db.add(c)

    db.commit()
    db.close()

    msg = "Database seeded successfully!"
    print(f"✅ {msg}")
    print(f"   Admin: {settings.ADMIN_EMAIL} / {settings.ADMIN_PASSWORD}")
    print("   Customer: customer@openbake.com / Customer@123")
    print(f"   Categories: {len(categories_data)}")
    print(f"   Products: {len(products_data)}")
    print(f"   Coupons: {len(coupons)}")
    return msg


if __name__ == "__main__":
    seed()
