# 🥐 Bakery Ordering System — Project Setup Guide

> Full setup instructions for all three parts of the platform: Android App, Web App, and Backend API.

---

## 📁 Monorepo Structure

```
bakery-ordering-system/
├── bakery-android/        # Android app (Kotlin + Jetpack Compose)
├── bakery-web/            # Web app (Next.js + Tailwind CSS)
└── bakery-backend/        # API server (FastAPI + PostgreSQL)
```

---

## Prerequisites (Install These First)

| Tool | Version | Download |
|------|---------|----------|
| Android Studio | Hedgehog 2023.1+ | https://developer.android.com/studio |
| JDK | 17+ | Bundled with Android Studio |
| Node.js | 18+ | https://nodejs.org |
| Python | 3.11+ | https://python.org |
| PostgreSQL | 15+ | https://postgresql.org |
| Git | Latest | https://git-scm.com |

---

&nbsp;

# Part 1 — Backend (FastAPI)

## 1.1 Clone & Navigate

```bash
git clone https://github.com/your-org/bakery-ordering-system.git
cd bakery-ordering-system/bakery-backend
```

## 1.2 Create a Virtual Environment

```bash
# Create
python -m venv venv

# Activate — macOS / Linux
source venv/bin/activate

# Activate — Windows
venv\Scripts\activate
```

## 1.3 Install Dependencies

```bash
pip install -r requirements.txt
```

**`requirements.txt`**
```
fastapi==0.111.0
uvicorn[standard]==0.29.0
sqlalchemy==2.0.30
alembic==1.13.1
psycopg2-binary==2.9.9
pydantic[email]==2.7.1
python-jose[cryptography]==3.3.0
passlib[bcrypt]==1.7.4
python-multipart==0.0.9
httpx==0.27.0
redis==5.0.4
celery==5.4.0
firebase-admin==6.5.0
python-dotenv==1.0.1
cloudinary==1.40.0
```

## 1.4 Set Up the Database

```bash
# Open psql
psql -U postgres

# Create the database
CREATE DATABASE bakery_db;
CREATE USER bakery_user WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE bakery_db TO bakery_user;
\q
```

## 1.5 Configure Environment Variables

Create a `.env` file in `bakery-backend/`:

```env
# App
APP_ENV=development
SECRET_KEY=your-super-secret-jwt-key-change-this
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=15
REFRESH_TOKEN_EXPIRE_DAYS=30

# Database
DATABASE_URL=postgresql://bakery_user:yourpassword@localhost:5432/bakery_db

# Redis
REDIS_URL=redis://localhost:6379

# Firebase (download from Firebase Console → Project Settings → Service Account)
FIREBASE_CREDENTIALS_PATH=./firebase-credentials.json

# Cloudinary (for image uploads)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# Payment (Razorpay)
RAZORPAY_KEY_ID=your_key_id
RAZORPAY_KEY_SECRET=your_key_secret

# Admin
ADMIN_EMAIL=admin@yourbakery.com
ADMIN_PASSWORD=Admin@1234
```

## 1.6 Run Database Migrations

```bash
# Initialize Alembic (first time only)
alembic init alembic

# Create the first migration
alembic revision --autogenerate -m "initial_schema"

# Apply migrations
alembic upgrade head
```

## 1.7 Start the Backend Server

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

The API will be live at:
- **API Base:** `http://localhost:8000`
- **Swagger Docs:** `http://localhost:8000/docs`
- **ReDoc:** `http://localhost:8000/redoc`

## 1.8 Folder Structure

```
bakery-backend/
├── app/
│   ├── main.py              # FastAPI app entry point
│   ├── config.py            # Settings loaded from .env
│   ├── database.py          # SQLAlchemy engine + session
│   ├── models/              # DB table definitions
│   │   ├── user.py
│   │   ├── product.py
│   │   ├── order.py
│   │   └── coupon.py
│   ├── schemas/             # Pydantic request/response models
│   │   ├── auth.py
│   │   ├── product.py
│   │   └── order.py
│   ├── routers/             # Route handlers
│   │   ├── auth.py
│   │   ├── products.py
│   │   ├── orders.py
│   │   ├── profile.py
│   │   └── admin.py
│   ├── services/            # Business logic
│   │   ├── auth_service.py
│   │   ├── order_service.py
│   │   └── notification_service.py
│   └── utils/
│       ├── jwt.py
│       └── firebase.py
├── alembic/                 # DB migration files
├── tests/
├── firebase-credentials.json
├── requirements.txt
└── .env
```

## 1.9 Sample `main.py`

```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.routers import auth, products, orders, profile, admin
from app.database import engine, Base

Base.metadata.create_all(bind=engine)

app = FastAPI(title="Bakery API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000"],  # add production URL later
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router, prefix="/api/auth", tags=["Auth"])
app.include_router(products.router, prefix="/api", tags=["Products"])
app.include_router(orders.router, prefix="/api", tags=["Orders"])
app.include_router(profile.router, prefix="/api", tags=["Profile"])
app.include_router(admin.router, prefix="/api/admin", tags=["Admin"])

@app.get("/")
def root():
    return {"message": "Bakery API is running 🥐"}
```

---

&nbsp;

# Part 2 — Web App (Next.js)

## 2.1 Navigate to Web Folder

```bash
cd ../bakery-web
```

## 2.2 Install Dependencies

```bash
npm install
```

**Key packages used:**
```json
{
  "dependencies": {
    "next": "14.2.3",
    "react": "^18",
    "react-dom": "^18",
    "tailwindcss": "^3.4.1",
    "axios": "^1.6.8",
    "zustand": "^4.5.2",
    "react-hook-form": "^7.51.3",
    "zod": "^3.23.4",
    "firebase": "^10.11.1",
    "lucide-react": "^0.378.0",
    "framer-motion": "^11.1.7",
    "react-hot-toast": "^2.4.1",
    "@tanstack/react-query": "^5.35.1"
  }
}
```

To install all at once:
```bash
npm install next react react-dom tailwindcss axios zustand react-hook-form zod firebase lucide-react framer-motion react-hot-toast @tanstack/react-query
```

## 2.3 Configure Environment Variables

Create `.env.local` in `bakery-web/`:

```env
# Backend API
NEXT_PUBLIC_API_URL=http://localhost:8000/api

# Firebase (from Firebase Console → Project Settings → Web App)
NEXT_PUBLIC_FIREBASE_API_KEY=your_api_key
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=your_project.firebaseapp.com
NEXT_PUBLIC_FIREBASE_PROJECT_ID=your_project_id
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=your_sender_id
NEXT_PUBLIC_FIREBASE_APP_ID=your_app_id

# Razorpay (public key only)
NEXT_PUBLIC_RAZORPAY_KEY_ID=your_razorpay_key_id
```

## 2.4 Configure Tailwind CSS

`tailwind.config.ts`:

```typescript
import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx}",
    "./components/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: "#C2773A",
        secondary: "#F4A261",
        cream: "#FFF8F2",
        accent: "#E76F51",
        border: "#E8DDD4",
      },
      fontFamily: {
        serif: ["Playfair Display", "serif"],
        sans: ["Nunito", "sans-serif"],
      },
    },
  },
  plugins: [],
};

export default config;
```

Add fonts to `app/layout.tsx`:

```typescript
import { Playfair_Display, Nunito } from "next/font/google";

const playfair = Playfair_Display({
  subsets: ["latin"],
  variable: "--font-playfair",
});

const nunito = Nunito({
  subsets: ["latin"],
  variable: "--font-nunito",
});
```

## 2.5 Folder Structure

```
bakery-web/
├── app/
│   ├── layout.tsx
│   ├── page.tsx                     # Homepage
│   ├── (auth)/
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── menu/
│   │   ├── page.tsx                 # All products
│   │   └── [id]/page.tsx            # Product detail
│   ├── cart/page.tsx
│   ├── checkout/page.tsx
│   ├── orders/
│   │   ├── page.tsx                 # Order history
│   │   └── [id]/page.tsx            # Order tracking
│   ├── profile/page.tsx
│   └── admin/
│       ├── layout.tsx
│       ├── dashboard/page.tsx
│       ├── products/page.tsx
│       └── orders/page.tsx
├── components/
│   ├── ui/                          # Button, Input, Badge, Modal, Toast
│   ├── layout/                      # Navbar, Footer, AdminSidebar
│   ├── product/                     # ProductCard, ProductGrid, ProductDetail
│   ├── cart/                        # CartDrawer, CartItem
│   └── order/                       # OrderCard, OrderTracker, StatusBadge
├── lib/
│   ├── api.ts                       # Axios instance with interceptors
│   ├── firebase.ts                  # Firebase init
│   └── utils.ts
├── store/
│   ├── cartStore.ts                 # Zustand cart state
│   └── authStore.ts                 # Zustand auth state
├── hooks/
│   ├── useProducts.ts
│   └── useOrders.ts
├── types/
│   └── index.ts
├── public/
│   └── images/
├── .env.local
├── tailwind.config.ts
└── next.config.mjs
```

## 2.6 Axios Setup (`lib/api.ts`)

```typescript
import axios from "axios";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  headers: { "Content-Type": "application/json" },
});

// Attach JWT token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem("access_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Auto-refresh on 401
api.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (error.response?.status === 401) {
      // handle token refresh or redirect to login
    }
    return Promise.reject(error);
  }
);

export default api;
```

## 2.7 Start the Dev Server

```bash
npm run dev
```

App runs at **`http://localhost:3000`**

## 2.8 Build for Production

```bash
npm run build
npm start
```

---

&nbsp;

# Part 3 — Android App (Kotlin + Jetpack Compose)

## 3.1 Open in Android Studio

1. Open Android Studio
2. Click **Open** → select `bakery-android/` folder
3. Wait for Gradle sync to complete

## 3.2 Project-level `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.google.services) apply false
}
```

## 3.3 App-level `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    kotlin("kapt")
}

android {
    compileSdk = 34
    defaultConfig {
        applicationId = "com.yourbakery.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/api/\"")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Retrofit + Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.1.1")

    // DataStore (local storage)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")
}
```

## 3.4 Configure Firebase for Android

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create a project → Add Android App
3. Enter package name: `com.yourbakery.app`
4. Download `google-services.json`
5. Place it in `bakery-android/app/google-services.json`

## 3.5 Set Base URL

In `app/src/main/java/com/yourbakery/app/data/api/RetrofitClient.kt`:

```kotlin
object RetrofitClient {
    // Use 10.0.2.2 to reach localhost from Android emulator
    // Change to your production URL before release
    private const val BASE_URL = BuildConfig.BASE_URL

    val instance: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(AuthInterceptor())  // attaches JWT token
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(ApiService::class.java)
    }
}
```

## 3.6 Folder Structure

```
bakery-android/
└── app/src/main/
    ├── java/com/yourbakery/app/
    │   ├── MainActivity.kt
    │   ├── BakeryApp.kt              # Hilt Application class
    │   ├── data/
    │   │   ├── api/
    │   │   │   ├── ApiService.kt     # Retrofit interface
    │   │   │   └── RetrofitClient.kt
    │   │   ├── model/
    │   │   │   ├── Product.kt
    │   │   │   ├── Order.kt
    │   │   │   ├── User.kt
    │   │   │   └── Cart.kt
    │   │   ├── repository/
    │   │   │   ├── AuthRepository.kt
    │   │   │   ├── ProductRepository.kt
    │   │   │   └── OrderRepository.kt
    │   │   └── local/
    │   │       └── TokenDataStore.kt
    │   ├── ui/
    │   │   ├── theme/
    │   │   │   ├── Color.kt
    │   │   │   ├── Type.kt
    │   │   │   └── Theme.kt
    │   │   ├── navigation/
    │   │   │   └── NavGraph.kt
    │   │   ├── auth/
    │   │   │   ├── LoginScreen.kt
    │   │   │   └── RegisterScreen.kt
    │   │   ├── home/
    │   │   │   └── HomeScreen.kt
    │   │   ├── product/
    │   │   │   ├── ProductListScreen.kt
    │   │   │   └── ProductDetailScreen.kt
    │   │   ├── cart/
    │   │   │   └── CartScreen.kt
    │   │   ├── checkout/
    │   │   │   └── CheckoutScreen.kt
    │   │   ├── orders/
    │   │   │   ├── OrderHistoryScreen.kt
    │   │   │   └── OrderTrackingScreen.kt
    │   │   └── profile/
    │   │       └── ProfileScreen.kt
    │   ├── viewmodel/
    │   │   ├── AuthViewModel.kt
    │   │   ├── ProductViewModel.kt
    │   │   ├── CartViewModel.kt
    │   │   └── OrderViewModel.kt
    │   └── util/
    │       ├── Constants.kt
    │       └── Resource.kt           # Sealed class: Loading/Success/Error
    ├── res/
    │   ├── values/
    │   │   ├── colors.xml
    │   │   └── strings.xml
    │   └── drawable/
└── google-services.json
```

## 3.7 Theme Setup (`ui/theme/Color.kt`)

```kotlin
import androidx.compose.ui.graphics.Color

val Primary = Color(0xFFC2773A)
val Secondary = Color(0xFFF4A261)
val Cream = Color(0xFFFFF8F2)
val Accent = Color(0xFFE76F51)
val TextPrimary = Color(0xFF1A1A1A)
val TextSecondary = Color(0xFF6B6B6B)
val Success = Color(0xFF52B788)
val Error = Color(0xFFE63946)
```

## 3.8 Run the App

1. Start the backend server first (`uvicorn app.main:app --reload`)
2. Start an Android Emulator (API 26+) or connect a physical device
3. Click **Run ▶** in Android Studio

> **Note:** The emulator uses `10.0.2.2` to reach your computer's `localhost`. If running on a physical device, replace the BASE_URL with your computer's local network IP (e.g., `http://192.168.1.x:8000/api/`).

---

&nbsp;

# Firebase Setup (Common for All Platforms)

1. Go to [Firebase Console](https://console.firebase.google.com) → Create project
2. Enable these services:
   - **Authentication** → Email/Password, Google, Phone
   - **Cloud Messaging** (for push notifications)
3. For Android: download `google-services.json` → place in `bakery-android/app/`
4. For Web: copy Firebase config → paste in `bakery-web/.env.local`
5. For Backend: download Service Account JSON → save as `bakery-backend/firebase-credentials.json`

---

&nbsp;

# Running Everything Together

Open 3 terminal windows:

```bash
# Terminal 1 — Backend
cd bakery-backend
source venv/bin/activate
uvicorn app.main:app --reload --port 8000

# Terminal 2 — Web
cd bakery-web
npm run dev

# Terminal 3 — Android (or just use Android Studio Run button)
# Open bakery-android/ in Android Studio and click Run ▶
```

| Service | URL |
|---------|-----|
| Backend API | http://localhost:8000 |
| Swagger Docs | http://localhost:8000/docs |
| Web App | http://localhost:3000 |
| Android | Emulator / Device |

---

&nbsp;

# Common Issues & Fixes

| Problem | Fix |
|---------|-----|
| Android can't reach backend | Use `10.0.2.2:8000` not `localhost:8000` in emulator |
| CORS error on web | Add `http://localhost:3000` to `allow_origins` in `main.py` |
| Alembic migration fails | Check `DATABASE_URL` in `.env` matches your PostgreSQL credentials |
| Firebase auth not working | Ensure `google-services.json` is in the correct `app/` folder |
| Gradle sync fails | File → Invalidate Caches → Restart in Android Studio |
| Port 8000 already in use | `lsof -ti:8000 | xargs kill` (macOS/Linux) |

---

&nbsp;

# Deployment Checklist

### Backend
- [ ] Set `APP_ENV=production` in `.env`
- [ ] Change `SECRET_KEY` to a strong random value
- [ ] Update `allow_origins` in CORS to your production domain
- [ ] Run migrations on production DB: `alembic upgrade head`
- [ ] Deploy to Railway / Render / EC2

### Web
- [ ] Set `NEXT_PUBLIC_API_URL` to production backend URL
- [ ] `npm run build` passes without errors
- [ ] Deploy to Vercel (connect GitHub repo, auto-deploys on push)

### Android
- [ ] Update `BASE_URL` in `build.gradle.kts` to production API URL
- [ ] Generate signed APK: Build → Generate Signed Bundle/APK
- [ ] Test on physical device before release
- [ ] Upload to Google Play Console

---

*Last updated: March 2026 | Bakery Ordering System v1.0*
