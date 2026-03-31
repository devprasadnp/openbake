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

    # Redis
    REDIS_URL: str = "redis://localhost:6379"

    # Firebase
    FIREBASE_CREDENTIALS_PATH: str = "./firebase-credentials.json"

    # Cloudinary
    CLOUDINARY_CLOUD_NAME: str = ""
    CLOUDINARY_API_KEY: str = ""
    CLOUDINARY_API_SECRET: str = ""

    # Razorpay
    RAZORPAY_KEY_ID: str = ""
    RAZORPAY_KEY_SECRET: str = ""

    # Bakery Location (Bangalore default)
    BAKERY_LAT: float = 12.9716
    BAKERY_LNG: float = 77.5946
    FREE_DELIVERY_RADIUS_KM: float = 5.0
    DELIVERY_FEE_DEFAULT: float = 40.0

    # Admin bootstrap credentials (used only by seed.py)
    ADMIN_EMAIL: str = "admin@openbake.in"
    ADMIN_PASSWORD: str = "Admin@1234"

    class Config:
        env_file = ".env"

    def validate_production(self):
        """Raise if critical settings are insecure in production."""
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
