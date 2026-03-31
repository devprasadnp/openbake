"""OpenBake FastAPI application — production-ready entry point."""
import logging
import time
import uuid

from fastapi import FastAPI, Request, Response, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address

from app.config import get_settings
from app.database import engine, Base, SessionLocal
from app.routers import auth, products, orders, profile, admin
from app.utils.logging import setup_logging, logger

# ── Bootstrap ─────────────────────────────────────────────────────────────────
settings = get_settings()
setup_logging(level="DEBUG" if settings.APP_ENV == "development" else "INFO")

# Auto-create tables (use Alembic migrations in production instead)
Base.metadata.create_all(bind=engine)

# ── Rate limiter ───────────────────────────────────────────────────────────────
limiter = Limiter(key_func=get_remote_address, default_limits=["200/minute"])

# ── App ────────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="OpenBake API",
    description="Bakery Ordering System REST API",
    version="1.0.0",
    docs_url="/docs" if settings.APP_ENV != "production" else None,
    redoc_url="/redoc" if settings.APP_ENV != "production" else None,
    openapi_url="/openapi.json" if settings.APP_ENV != "production" else None,
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# ── CORS ───────────────────────────────────────────────────────────────────────
_raw_origins = settings.ALLOWED_ORIGINS
allowed_origins = [o.strip() for o in _raw_origins.split(",") if o.strip()] if _raw_origins else ["*"]

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type", "Accept"],
    expose_headers=["X-Request-ID"],
)

# ── Request-ID + timing middleware ─────────────────────────────────────────────
@app.middleware("http")
async def request_middleware(request: Request, call_next) -> Response:
    request_id = str(uuid.uuid4())
    start = time.perf_counter()
    request.state.request_id = request_id

    response = await call_next(request)

    elapsed_ms = round((time.perf_counter() - start) * 1000, 2)
    response.headers["X-Request-ID"] = request_id
    response.headers["X-Response-Time"] = f"{elapsed_ms}ms"

    logger.info(
        "request",
        extra={
            "request_id": request_id,
            "method": request.method,
            "path": request.url.path,
            "status_code": response.status_code,
            "duration_ms": elapsed_ms,
        },
    )
    return response


# ── Global exception handler ──────────────────────────────────────────────────
@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    request_id = getattr(request.state, "request_id", "unknown")
    logger.error(
        "unhandled_exception",
        exc_info=exc,
        extra={"request_id": request_id, "path": request.url.path},
    )
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "detail": "An internal server error occurred.",
            "request_id": request_id,
        },
    )


# ── Routers ────────────────────────────────────────────────────────────────────
app.include_router(auth.router,     prefix="/api/auth",  tags=["Auth"])
app.include_router(products.router, prefix="/api",       tags=["Products"])
app.include_router(orders.router,   prefix="/api",       tags=["Orders"])
app.include_router(profile.router,  prefix="/api",       tags=["Profile"])
app.include_router(admin.router,    prefix="/api/admin", tags=["Admin"])


# ── Health & root ──────────────────────────────────────────────────────────────
@app.get("/", include_in_schema=False)
def root():
    return {"message": "OpenBake API is running 🥐"}


@app.get("/health", tags=["Health"])
def health_check():
    """Liveness + DB connectivity probe used by load balancers / orchestrators."""
    db_ok = False
    db = SessionLocal()
    try:
        db.execute(__import__("sqlalchemy").text("SELECT 1"))
        db_ok = True
    except Exception as exc:
        logger.warning("health_check_db_failed", extra={"error": str(exc)})
    finally:
        db.close()

    http_status = status.HTTP_200_OK if db_ok else status.HTTP_503_SERVICE_UNAVAILABLE
    return JSONResponse(
        status_code=http_status,
        content={
            "status": "healthy" if db_ok else "degraded",
            "database": "ok" if db_ok else "unreachable",
            "version": "1.0.0",
            "env": settings.APP_ENV,
        },
    )
