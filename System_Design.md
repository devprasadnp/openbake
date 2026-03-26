# System Design Document
## Bakery Ordering System

**Version:** 1.0

---

## 1. Architecture Overview

The system follows a standard 3-tier architecture:

```
[ Mobile App (Android) ]     [ Web App (Next.js) ]
              |                        |
              |_______ REST API ________|
                           |
                    [ Backend (FastAPI) ]
                           |
              _____________|_____________
             |             |             |
       [ PostgreSQL ]  [ Redis ]   [ Firebase ]
        (main DB)      (cache)     (auth + push)
                           |
                    [ File Storage ]
                     (images/media)
```

---

## 2. Tech Stack

### Mobile App
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: MVVM
- Networking: Retrofit + OkHttp
- Async: Coroutines + Flow
- Auth & Notifications: Firebase

### Web App
- Framework: Next.js 14 (App Router)
- Styling: Tailwind CSS
- State: React Context / Zustand
- HTTP: Axios

### Backend
- Framework: FastAPI (Python)
- Auth: JWT (access + refresh tokens)
- Database ORM: SQLAlchemy
- Cache: Redis (sessions, rate limiting)
- Background Jobs: Celery (notifications, order updates)

### Database
- Primary: PostgreSQL
- Cache: Redis

### Infrastructure
- Auth: Firebase Authentication
- Push Notifications: Firebase Cloud Messaging (FCM)
- File Storage: AWS S3 or Cloudinary (product images)
- Hosting: Railway / Render (backend), Vercel (frontend)

---

## 3. Database Schema

### users
```
id              UUID        PRIMARY KEY
name            VARCHAR
email           VARCHAR     UNIQUE
phone           VARCHAR     UNIQUE
password_hash   VARCHAR
auth_provider   VARCHAR     (email | google | phone)
role            VARCHAR     (customer | admin)
created_at      TIMESTAMP
```

### addresses
```
id              UUID        PRIMARY KEY
user_id         UUID        FOREIGN KEY → users
label           VARCHAR     (Home, Work, Other)
full_address    TEXT
city            VARCHAR
pincode         VARCHAR
lat             FLOAT
lng             FLOAT
is_default      BOOLEAN
```

### categories
```
id              UUID        PRIMARY KEY
name            VARCHAR
image_url       VARCHAR
is_active       BOOLEAN
```

### products
```
id              UUID        PRIMARY KEY
category_id     UUID        FOREIGN KEY → categories
name            VARCHAR
description     TEXT
price           DECIMAL
images          TEXT[]      (array of image URLs)
is_available    BOOLEAN
is_eggless_available  BOOLEAN
customizable    BOOLEAN
stock_count     INTEGER
rating          FLOAT
```

### product_variants
```
id              UUID        PRIMARY KEY
product_id      UUID        FOREIGN KEY → products
variant_type    VARCHAR     (size | flavor)
value           VARCHAR     (e.g. 0.5kg, 1kg | chocolate, vanilla)
extra_price     DECIMAL
```

### orders
```
id              UUID        PRIMARY KEY
user_id         UUID        FOREIGN KEY → users
address_id      UUID        FOREIGN KEY → addresses
order_type      VARCHAR     (delivery | pickup)
status          VARCHAR     (placed | accepted | preparing | dispatched | delivered | cancelled)
subtotal        DECIMAL
discount        DECIMAL
delivery_fee    DECIMAL
total           DECIMAL
coupon_code     VARCHAR
payment_method  VARCHAR
payment_status  VARCHAR     (pending | paid | failed)
scheduled_date  DATE
time_slot       VARCHAR     (e.g. 10AM-12PM)
special_note    TEXT
created_at      TIMESTAMP
updated_at      TIMESTAMP
```

### order_items
```
id              UUID        PRIMARY KEY
order_id        UUID        FOREIGN KEY → orders
product_id      UUID        FOREIGN KEY → products
quantity        INTEGER
unit_price      DECIMAL
customization   JSONB       (eggless, size, flavor, cake_message)
```

### coupons
```
id              UUID        PRIMARY KEY
code            VARCHAR     UNIQUE
discount_type   VARCHAR     (flat | percent)
discount_value  DECIMAL
min_order_value DECIMAL
max_uses        INTEGER
used_count      INTEGER
valid_from      DATE
valid_until     DATE
is_active       BOOLEAN
```

### reviews
```
id              UUID        PRIMARY KEY
user_id         UUID        FOREIGN KEY → users
product_id      UUID        FOREIGN KEY → products
order_id        UUID        FOREIGN KEY → orders
rating          INTEGER     (1–5)
comment         TEXT
created_at      TIMESTAMP
```

### wishlist
```
id              UUID        PRIMARY KEY
user_id         UUID        FOREIGN KEY → users
product_id      UUID        FOREIGN KEY → products
created_at      TIMESTAMP
```

---

## 4. API Endpoints

### Auth
```
POST   /api/auth/register          Register with email/password
POST   /api/auth/login             Login with email/password
POST   /api/auth/google            Google OAuth login
POST   /api/auth/phone/send-otp    Send OTP to phone
POST   /api/auth/phone/verify-otp  Verify OTP and get token
POST   /api/auth/refresh           Refresh access token
POST   /api/auth/logout            Logout
```

### Products
```
GET    /api/categories             List all categories
GET    /api/products               List products (filter: category, search, page)
GET    /api/products/:id           Get product details
GET    /api/products/:id/reviews   Get product reviews
```

### Cart (client-side state; server validates at checkout)
```
POST   /api/cart/validate          Validate cart items (prices, availability)
POST   /api/coupons/apply          Apply coupon code
```

### Orders
```
POST   /api/orders                 Place an order
GET    /api/orders                 Get user order history
GET    /api/orders/:id             Get order details + status
PATCH  /api/orders/:id/cancel      Cancel an order
```

### User Profile
```
GET    /api/profile                Get user profile
PATCH  /api/profile                Update profile
GET    /api/addresses              List saved addresses
POST   /api/addresses              Add new address
DELETE /api/addresses/:id          Delete address
POST   /api/reviews                Submit a review
GET    /api/wishlist               Get wishlist
POST   /api/wishlist/:productId    Add to wishlist
DELETE /api/wishlist/:productId    Remove from wishlist
```

### Admin
```
GET    /api/admin/dashboard        Sales summary (today, week, month)
GET    /api/admin/orders           List all orders (filter by status, date)
PATCH  /api/admin/orders/:id       Update order status
GET    /api/admin/products         List products
POST   /api/admin/products         Create product
PATCH  /api/admin/products/:id     Update product
DELETE /api/admin/products/:id     Delete product
GET    /api/admin/inventory        Items with low stock
POST   /api/admin/coupons          Create coupon
GET    /api/admin/coupons          List coupons
PATCH  /api/admin/coupons/:id      Update coupon
```

---

## 5. Folder Structure

### Backend (FastAPI)
```
bakery-backend/
├── app/
│   ├── main.py
│   ├── config.py
│   ├── database.py
│   ├── models/
│   │   ├── user.py
│   │   ├── product.py
│   │   ├── order.py
│   │   └── coupon.py
│   ├── schemas/
│   │   ├── user.py
│   │   ├── product.py
│   │   └── order.py
│   ├── routers/
│   │   ├── auth.py
│   │   ├── products.py
│   │   ├── orders.py
│   │   ├── profile.py
│   │   └── admin.py
│   ├── services/
│   │   ├── auth_service.py
│   │   ├── order_service.py
│   │   └── notification_service.py
│   └── utils/
│       ├── jwt.py
│       └── firebase.py
├── requirements.txt
└── .env
```

### Web App (Next.js)
```
bakery-web/
├── app/
│   ├── (auth)/
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── (shop)/
│   │   ├── page.tsx              (home)
│   │   ├── categories/page.tsx
│   │   ├── products/[id]/page.tsx
│   │   ├── cart/page.tsx
│   │   └── checkout/page.tsx
│   ├── orders/
│   │   ├── page.tsx              (order history)
│   │   └── [id]/page.tsx         (order tracking)
│   └── admin/
│       ├── dashboard/page.tsx
│       ├── products/page.tsx
│       └── orders/page.tsx
├── components/
│   ├── ui/                       (buttons, inputs, modals)
│   ├── product/                  (ProductCard, ProductDetail)
│   ├── cart/                     (CartDrawer, CartItem)
│   └── order/                    (OrderCard, StatusTracker)
├── lib/
│   ├── api.ts                    (axios instance)
│   └── utils.ts
├── store/                        (cart, auth state)
└── public/
```

### Mobile App (Android)
```
bakery-android/
└── app/src/main/
    ├── data/
    │   ├── api/
    │   │   ├── ApiService.kt
    │   │   └── RetrofitClient.kt
    │   ├── model/
    │   │   ├── Product.kt
    │   │   ├── Order.kt
    │   │   └── User.kt
    │   └── repository/
    │       ├── AuthRepository.kt
    │       ├── ProductRepository.kt
    │       └── OrderRepository.kt
    ├── ui/
    │   ├── auth/
    │   │   ├── LoginScreen.kt
    │   │   └── RegisterScreen.kt
    │   ├── home/
    │   │   └── HomeScreen.kt
    │   ├── product/
    │   │   ├── ProductListScreen.kt
    │   │   └── ProductDetailScreen.kt
    │   ├── cart/
    │   │   └── CartScreen.kt
    │   ├── checkout/
    │   │   └── CheckoutScreen.kt
    │   └── orders/
    │       ├── OrderHistoryScreen.kt
    │       └── OrderTrackingScreen.kt
    ├── viewmodel/
    │   ├── AuthViewModel.kt
    │   ├── ProductViewModel.kt
    │   ├── CartViewModel.kt
    │   └── OrderViewModel.kt
    └── util/
        ├── Constants.kt
        └── TokenManager.kt
```

---

## 6. Order Status Flow

```
Customer places order
        ↓
    [PLACED]  → Admin receives notification
        ↓
   [ACCEPTED] → Customer notified
        ↓
  [PREPARING] → Kitchen starts working
        ↓
 [DISPATCHED] → Delivery assigned (or ready for pickup)
        ↓
  [DELIVERED] → Customer can review
```

---

## 7. Key Design Decisions

- **Cart is client-side** — stored in app/browser state. Validated on the server only at checkout. This reduces API calls and keeps the cart fast.
- **JWT with refresh tokens** — short-lived access tokens (15 min) + long-lived refresh tokens (30 days). Stored securely.
- **Redis for caching** — popular product lists and category data cached for 5 minutes to reduce DB load.
- **FCM for all notifications** — single push notification service for both app and web.
- **Image uploads via Cloudinary** — avoids managing file storage on the backend server.
