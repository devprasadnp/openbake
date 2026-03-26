"""
Notification service — handles push notifications via Firebase Cloud Messaging.
"""
from app.utils.firebase import send_push_notification


ORDER_STATUS_MESSAGES = {
    "accepted": ("Order Accepted! ✅", "Your order has been accepted and will be prepared shortly."),
    "preparing": ("Being Prepared 👨‍🍳", "Your order is now being prepared by our bakers."),
    "dispatched": ("On Its Way! 🚗", "Your order has been dispatched and is on its way to you."),
    "delivered": ("Delivered! 🎉", "Your order has been delivered. Enjoy your treats!"),
    "cancelled": ("Order Cancelled ❌", "Your order has been cancelled."),
}


def notify_order_status(device_token: str, order_id: str, status: str):
    """Send a push notification for an order status change."""
    if status in ORDER_STATUS_MESSAGES:
        title, body = ORDER_STATUS_MESSAGES[status]
        send_push_notification(
            token=device_token,
            title=title,
            body=body,
            data={"order_id": order_id, "status": status},
        )
