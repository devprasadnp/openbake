from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # App
    APP_ENV: str = "development"
    SECRET_KEY: str = "change-this-to-a-long-random-secret-in-production"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 15
    REFRESH_TOKEN_EXPIRE_DAYS: int = 30

    # CORS — comma-separated list of allowed origins. Use "*" only in dev.
    ALLOWED_ORIGINS: str = "*"

    # Database
    DATABASE_URL: str = "sqlite:///./openbake.db"
    POSTGRES_URL: str | None = None

    # Redis
    REDIS_URL: str = "redis://localhost:6379"

    # Firebase
    FIREBASE_CREDENTIALS_PATH: str = "./firebase-credentials.json"

    # Cloudinary
    CLOUDINARY_CLOUD_NAME: str = ""
    CLOUDINARY_API_KEY: str = ""
    CLOUDINARY_API_SECRET: str = ""

    # Razorpay (legacy fallback)
    RAZORPAY_KEY_ID: str = ""
    RAZORPAY_KEY_SECRET: str = ""

    # PayU
    PAYU_MERCHANT_KEY: str = "PgFuTw"
    PAYU_MERCHANT_SALT: str = "vVRWH3BsZiAezWr1xEsFHvdcs9zf6m2j"
    PAYU_CLIENT_ID: str = "62caa8f1511e0d506cb3c5a5babadf5aecea698911f8621159127b820ece1610"
    PAYU_CLIENT_SECRET: str = "c039351a08f886523344d6693a88ac542d158922f7cdd56cd4e0187aeb4084d5"
    PAYU_PAYMENT_URL: str = "https://test.payu.in/_payment"
    PAYU_STATUS_API_URL: str = "https://test.payu.in/merchant/postservice?form=2"
    PAYU_CALLBACK_BASE_URL: str = ""

    # Redirect targets
    WEB_BASE_URL: str = "http://localhost:3000"
    ANDROID_DEEP_LINK_BASE: str = "openbake://payment-result"

    # Bakery Location (Bangalore default)
    BAKERY_LAT: float = 12.9716
    BAKERY_LNG: float = 77.5946
    FREE_DELIVERY_RADIUS_KM: float = 5.0
    DELIVERY_FEE_DEFAULT: float = 40.0

    # Admin bootstrap credentials (used only by seed.py)
    ADMIN_EMAIL: str = "admin@srivinayakabakery.in"
    ADMIN_PASSWORD: str = "Admin@1234"

    class Config:
        env_file = ".env"

    def validate_production(self):
        """Raise if critical settings are insecure in production."""
        # Vercel Postgres support: Use POSTGRES_URL if available
        if self.POSTGRES_URL and "sqlite" in self.DATABASE_URL:
            self.DATABASE_URL = self.POSTGRES_URL
            
        # SQLAlchemy 2.0 requires postgresql:// instead of postgres://
        if self.DATABASE_URL.startswith("postgres://"):
            self.DATABASE_URL = self.DATABASE_URL.replace("postgres://", "postgresql://", 1)
            
        if self.APP_ENV == "production":
            if self.SECRET_KEY == "change-this-to-a-long-random-secret-in-production":
                raise ValueError(
                    "FATAL: SECRET_KEY must be changed from default in production. "
                    "Set a strong random secret via the SECRET_KEY environment variable."
                )


@lru_cache()
def get_settings() -> Settings:
    s = Settings()
    s.validate_production()
    return s
