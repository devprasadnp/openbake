from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import auth, products, orders, profile, admin
from app.database import engine, Base

# Create all tables (development only — use Alembic migrations in production)
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="OpenBake API",
    description="Bakery Ordering System REST API",
    version="1.0.0",
)

# CORS — allow local development origins
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:3000",   # Next.js dev
        "http://127.0.0.1:3000",
        "http://10.0.2.2:8000",    # Android emulator
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount routers
app.include_router(auth.router, prefix="/api/auth", tags=["Auth"])
app.include_router(products.router, prefix="/api", tags=["Products"])
app.include_router(orders.router, prefix="/api", tags=["Orders"])
app.include_router(profile.router, prefix="/api", tags=["Profile"])
app.include_router(admin.router, prefix="/api/admin", tags=["Admin"])


@app.get("/")
def root():
    return {"message": "OpenBake API is running 🥐"}


@app.get("/health")
def health_check():
    return {"status": "healthy"}
