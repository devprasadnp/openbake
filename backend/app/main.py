"""OpenBake FastAPI application — production-ready entry point."""
import logging
import os
import re
import time
import uuid
from urllib.parse import urlsplit

from fastapi import FastAPI, Request, Response, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address

from app.config import get_settings
from app.database import engine, Base, SessionLocal
from app.routers import auth, products, orders, profile, admin, payments, delivery, waitlist
from app.utils.logging import setup_logging, logger

# ── Bootstrap ─────────────────────────────────────────────────────────────────
settings = get_settings()
setup_logging(level="DEBUG" if settings.APP_ENV == "development" else "INFO")

# Auto-create tables (use Alembic migrations in production instead)
try:
    Base.metadata.create_all(bind=engine)
    
    # Auto-seed the database if it is empty
    from app.seed import seed
    seed()
except Exception as e:
    logger.warning("Could not auto-create tables or seed database", extra={"error": str(e)})

# ── Rate limiter ───────────────────────────────────────────────────────────────
limiter = Limiter(key_func=get_remote_address, default_limits=["200/minute"])

# ── App ────────────────────────────────────────────────────────────────────────
app = FastAPI(
    title="Sri Vinayaka Bakery API",
    description="Bakery Ordering System REST API",
    version="1.0.0",
    docs_url="/docs" if settings.APP_ENV != "production" else None,
    redoc_url="/redoc" if settings.APP_ENV != "production" else None,
    openapi_url="/openapi.json" if settings.APP_ENV != "production" else None,
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

# ── CORS ───────────────────────────────────────────────────────────────────────
def _normalize_origin(origin: str) -> str:
    candidate = origin.strip().rstrip("/")
    if not candidate or candidate == "*":
        return candidate

    parsed = urlsplit(candidate)
    if parsed.scheme and parsed.netloc:
        return f"{parsed.scheme}://{parsed.netloc}"

    return candidate


def _build_cors_config(raw_origins: str, app_env: str) -> tuple[list[str], str | None]:
    # In development, allow browser access from any local tunnel or frontend host.
    if app_env == "development":
        return ["*"], None

    explicit_origins: list[str] = []
    wildcard_patterns: list[str] = []

    for origin in raw_origins.split(",") if raw_origins else ["*"]:
        normalized = _normalize_origin(origin)
        if not normalized:
            continue

        if normalized == "*":
            return [], ".*"

        if "*" in normalized:
            wildcard_patterns.append("^" + re.escape(normalized).replace(r"\*", ".*") + "$")
            continue

        explicit_origins.append(normalized)

    deduped_origins = list(dict.fromkeys(explicit_origins))
    wildcard_regex = "|".join(wildcard_patterns) if wildcard_patterns else None
    return deduped_origins, wildcard_regex


allowed_origins, allowed_origin_regex = _build_cors_config(
    settings.ALLOWED_ORIGINS,
    settings.APP_ENV,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_origin_regex=allowed_origin_regex,
    allow_credentials=len(allowed_origins) != 1 or allowed_origins[0] != "*",
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["X-Request-ID", "X-Response-Time"],
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
    # In development, include the actual error message for easier debugging
    detail = str(exc) if settings.APP_ENV == "development" else "An internal server error occurred."
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "detail": detail,
            "request_id": request_id,
        },
    )


# ── Routers ────────────────────────────────────────────────────────────────────
app.include_router(auth.router,     prefix="/api/auth",  tags=["Auth"])
app.include_router(products.router, prefix="/api",       tags=["Products"])
app.include_router(orders.router,   prefix="/api",       tags=["Orders"])
app.include_router(profile.router,  prefix="/api",       tags=["Profile"])
app.include_router(payments.router, prefix="/api",       tags=["Payments"])
app.include_router(delivery.router, prefix="/api",       tags=["Delivery"])
app.include_router(waitlist.router, prefix="/api",       tags=["Waitlist"])
app.include_router(admin.router,    prefix="/api/admin", tags=["Admin"])

# ── Static files (media uploads) ───────────────────────────────────────────────
_media_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "media")
try:
    os.makedirs(_media_dir, exist_ok=True)
    app.mount("/media", StaticFiles(directory=_media_dir), name="media")
except Exception as e:
    logger.warning("Could not create/mount media directory (read-only filesystem)", extra={"error": str(e)})


# ── Health & root ──────────────────────────────────────────────────────────────
@app.get("/", include_in_schema=False)
def root():
    return {"message": "Sri Vinayaka Bakery API is running 🍰"}


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
