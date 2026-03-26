"""
Firebase utility — initializes Firebase Admin SDK for auth verification and FCM push notifications.
"""
import os
from app.config import get_settings

settings = get_settings()

# Firebase Admin SDK initialization (lazy)
_firebase_app = None


def get_firebase_app():
    """Initialize Firebase Admin SDK if not already done."""
    global _firebase_app
    if _firebase_app is None:
        try:
            import firebase_admin
            from firebase_admin import credentials

            cred_path = settings.FIREBASE_CREDENTIALS_PATH
            if os.path.exists(cred_path):
                cred = credentials.Certificate(cred_path)
                _firebase_app = firebase_admin.initialize_app(cred)
            else:
                print(
                    f"⚠️  Firebase credentials not found at {cred_path}. "
                    "Firebase features (Google auth, push notifications) will be unavailable."
                )
        except Exception as e:
            print(f"⚠️  Failed to initialize Firebase: {e}")
    return _firebase_app


def verify_google_id_token(id_token: str) -> dict:
    """Verify a Google ID token via Firebase Auth and return user info."""
    from firebase_admin import auth

    get_firebase_app()
    decoded = auth.verify_id_token(id_token)
    return {
        "uid": decoded["uid"],
        "email": decoded.get("email"),
        "name": decoded.get("name", ""),
        "picture": decoded.get("picture", ""),
    }


def send_push_notification(token: str, title: str, body: str, data: dict = None):
    """Send an FCM push notification to a device."""
    from firebase_admin import messaging

    get_firebase_app()
    message = messaging.Message(
        notification=messaging.Notification(title=title, body=body),
        data=data or {},
        token=token,
    )
    try:
        messaging.send(message)
    except Exception as e:
        print(f"⚠️  Failed to send push notification: {e}")
