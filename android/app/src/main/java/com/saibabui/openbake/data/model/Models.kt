package com.saibabui.openbake.data.model

import com.google.gson.annotations.SerializedName

// ── Auth ──
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class GoogleAuthRequest(
    @SerializedName("id_token") val idToken: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class TokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String
)

// ── User ──
data class User(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    @SerializedName("auth_provider") val authProvider: String = "email",
    val role: String = "customer",
    @SerializedName("profile_image_url") val profileImageUrl: String? = null
)

data class UpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null
)

data class Address(
    val id: String,
    val label: String,
    @SerializedName("recipient_name") val recipientName: String? = null,
    @SerializedName("recipient_phone") val recipientPhone: String? = null,
    @SerializedName("house_number") val houseNumber: String? = null,
    val street: String? = null,
    val landmark: String? = null,
    @SerializedName("full_address") val fullAddress: String,
    val city: String,
    val state: String? = null,
    val pincode: String,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerializedName("is_default") val isDefault: Boolean = false
)

data class AddressRequest(
    val label: String,
    @SerializedName("recipient_name") val recipientName: String? = null,
    @SerializedName("recipient_phone") val recipientPhone: String? = null,
    @SerializedName("house_number") val houseNumber: String? = null,
    val street: String? = null,
    val landmark: String? = null,
    @SerializedName("full_address") val fullAddress: String,
    val city: String,
    val state: String? = null,
    val pincode: String,
    @SerializedName("is_default") val isDefault: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null
)

// ── Paginated Response ──
data class PaginatedProductResponse(
    val items: List<Product>,
    val total: Int,
    val page: Int,
    @SerializedName("page_size") val pageSize: Int,
    val pages: Int,
    @SerializedName("has_next") val hasNext: Boolean,
    @SerializedName("has_prev") val hasPrev: Boolean
)

// ── Product ──
data class Category(
    val id: String,
    val name: String,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class Product(
    val id: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    val images: List<String> = emptyList(),
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("is_available") val isAvailable: Boolean = true,
    @SerializedName("is_eggless_available") val isEgglessAvailable: Boolean = false,
    val customizable: Boolean = false,
    @SerializedName("stock_count") val stockCount: Int = 0,
    val rating: Double = 0.0,
    val variants: List<ProductVariant> = emptyList()
)

data class ProductVariant(
    val id: String,
    @SerializedName("variant_type") val variantType: String,
    val value: String,
    @SerializedName("extra_price") val extraPrice: Double = 0.0
)

// ── Order ──
data class CreateOrderRequest(
    @SerializedName("address_id") val addressId: String? = null,
    @SerializedName("order_type") val orderType: String = "delivery",
    val items: List<OrderItemRequest>,
    @SerializedName("payment_method") val paymentMethod: String = "cod",
    @SerializedName("coupon_code") val couponCode: String? = null,
    @SerializedName("scheduled_date") val scheduledDate: String? = null,
    @SerializedName("time_slot") val timeSlot: String? = null,
    @SerializedName("special_note") val specialNote: String? = null,
    @SerializedName("idempotency_key") val idempotencyKey: String? = null
)

data class OrderItemRequest(
    @SerializedName("product_id") val productId: String,
    val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    val customization: Map<String, String>? = null
)

data class Order(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("address_id") val addressId: String? = null,
    val address: Address? = null,
    @SerializedName("order_type") val orderType: String,
    val status: String,
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double,
    @SerializedName("delivery_fee") val deliveryFee: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double,
    @SerializedName("coupon_code") val couponCode: String? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("payment_status") val paymentStatus: String = "pending",
    @SerializedName("razorpay_order_id") val razorpayOrderId: String? = null,
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String? = null,
    @SerializedName("estimated_delivery_minutes") val estimatedDeliveryMinutes: Int? = null,
    @SerializedName("scheduled_date") val scheduledDate: String? = null,
    @SerializedName("time_slot") val timeSlot: String? = null,
    @SerializedName("special_note") val specialNote: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("status_timestamps") val statusTimestamps: Map<String, String>? = null
)

data class OrderItem(
    val id: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("product_name") val productName: String? = null,
    val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    val customization: Map<String, String>? = null
)

// ── Review ──
data class Review(
    val id: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("order_id") val orderId: String? = null,
    val rating: Int,
    val comment: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ReviewRequest(
    @SerializedName("product_id") val productId: String,
    @SerializedName("order_id") val orderId: String,
    val rating: Int,
    val comment: String? = null
)

// ── Cart (local only) ──
data class CartItem(
    val product: Product,
    val quantity: Int,
    val selectedVariant: ProductVariant? = null,
    val isEggless: Boolean = false,
    val cakeMessage: String? = null
) {
    val itemPrice: Double
        get() = product.price + (selectedVariant?.extraPrice ?: 0.0)
    val totalPrice: Double
        get() = itemPrice * quantity
}

// ── Wishlist ──
data class WishlistItem(
    val id: String,
    @SerializedName("product_id") val productId: String,
    val product: Product
)

// ── Delivery Estimate ──
data class DeliveryEstimate(
    @SerializedName("distance_km") val distanceKm: Double,
    @SerializedName("delivery_fee") val deliveryFee: Double,
    @SerializedName("estimated_time_minutes") val estimatedTimeMinutes: Int,
    @SerializedName("is_free_delivery") val isFreeDelivery: Boolean = false,
    @SerializedName("is_deliverable") val isDeliverable: Boolean
)

// ── Payment ──
data class CreatePaymentOrderRequest(
    @SerializedName("order_id") val orderId: String
)

data class RazorpayOrderResponse(
    @SerializedName("razorpay_order_id") val razorpayOrderId: String,
    @SerializedName("razorpay_key_id") val razorpayKeyId: String,
    @SerializedName("amount") val amount: Int,
    @SerializedName("currency") val currency: String,
    @SerializedName("order_id") val orderId: String
)

data class VerifyPaymentRequest(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("razorpay_order_id") val razorpayOrderId: String,
    @SerializedName("razorpay_payment_id") val razorpayPaymentId: String,
    @SerializedName("razorpay_signature") val razorpaySignature: String
)

data class PaymentVerifyResponse(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("payment_status") val paymentStatus: String,
    val message: String? = null
)

// ── Stock Waitlist ──
data class StockWaitlistItem(
    val id: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("variant_id") val variantId: String? = null,
    val status: String,
    @SerializedName("created_at") val createdAt: String? = null
)

data class StockWaitlistResponse(
    val id: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("variant_id") val variantId: String? = null,
    val status: String,
    @SerializedName("created_at") val createdAt: String? = null,
    val product: Product? = null
)

// ── Coupon ──
data class CouponApplyRequest(
    val code: String,
    val subtotal: Double
)

data class CouponApplyResponse(
    val valid: Boolean,
    val discount: Double = 0.0,
    val message: String = ""
)

// ── SSE Order Event ──
data class OrderStatusEvent(
    val status: String,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("estimated_delivery_minutes") val estimatedDeliveryMinutes: Int? = null
)

// ── OTP Auth ──
data class OtpSendRequest(
    val phone: String
)

data class OtpSendResponse(
    val message: String,
    @SerializedName("expires_in") val expiresIn: Int? = null
)

data class OtpVerifyRequest(
    val phone: String,
    val otp: String,
    val name: String? = null
)

// ── Admin Dashboard ──
data class DashboardStats(
    @SerializedName("today_orders") val todayOrders: Int = 0,
    @SerializedName("today_revenue") val todayRevenue: Double = 0.0,
    @SerializedName("week_orders") val weekOrders: Int = 0,
    @SerializedName("week_revenue") val weekRevenue: Double = 0.0,
    @SerializedName("month_orders") val monthOrders: Int = 0,
    @SerializedName("month_revenue") val monthRevenue: Double = 0.0,
    @SerializedName("pending_orders") val pendingOrders: Int = 0
)

// ── Admin Analytics ──
data class AnalyticsData(
    @SerializedName("daily_trend") val dailyTrend: List<DailyStat> = emptyList(),
    @SerializedName("status_breakdown") val statusBreakdown: List<StatusCount> = emptyList(),
    @SerializedName("order_type_split") val orderTypeSplit: List<TypeCount> = emptyList(),
    @SerializedName("top_products") val topProducts: List<TopProduct> = emptyList(),
    @SerializedName("payment_split") val paymentSplit: List<PaymentCount> = emptyList()
)

data class DailyStat(val date: String, val orders: Int = 0, val revenue: Double = 0.0)
data class StatusCount(val status: String, val count: Int = 0)
data class TypeCount(val type: String, val count: Int = 0)
data class TopProduct(val name: String, val units: Int = 0, val revenue: Double = 0.0)
data class PaymentCount(val method: String, val count: Int = 0)

// ── Admin Order Detail (with customer info) ──
data class AdminOrderDetail(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("address_id") val addressId: String? = null,
    val address: Address? = null,
    @SerializedName("order_type") val orderType: String = "delivery",
    val status: String = "placed",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    @SerializedName("delivery_fee") val deliveryFee: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    @SerializedName("coupon_code") val couponCode: String? = null,
    @SerializedName("payment_method") val paymentMethod: String? = null,
    @SerializedName("payment_status") val paymentStatus: String = "pending",
    @SerializedName("estimated_delivery_minutes") val estimatedDeliveryMinutes: Int? = null,
    @SerializedName("scheduled_date") val scheduledDate: String? = null,
    @SerializedName("time_slot") val timeSlot: String? = null,
    @SerializedName("special_note") val specialNote: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("status_timestamps") val statusTimestamps: Map<String, String>? = null,
    val customer: CustomerInfo? = null
)

data class CustomerInfo(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    @SerializedName("profile_image_url") val profileImageUrl: String? = null
)

// ── Admin Order Status Update ──
data class OrderStatusUpdateRequest(val status: String)

// ── Admin Product Create/Update ──
data class ProductCreateRequest(
    @SerializedName("category_id") val categoryId: String,
    val name: String,
    val description: String? = null,
    val price: Double,
    val images: List<String> = emptyList(),
    @SerializedName("is_available") val isAvailable: Boolean = true,
    @SerializedName("is_eggless_available") val isEgglessAvailable: Boolean = false,
    val customizable: Boolean = false,
    @SerializedName("stock_count") val stockCount: Int = 0,
    val variants: List<VariantRequest> = emptyList()
)

data class ProductUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val images: List<String>? = null,
    @SerializedName("is_available") val isAvailable: Boolean? = null,
    @SerializedName("is_eggless_available") val isEgglessAvailable: Boolean? = null,
    val customizable: Boolean? = null,
    @SerializedName("stock_count") val stockCount: Int? = null
)

data class VariantRequest(
    @SerializedName("variant_type") val variantType: String,
    val value: String,
    @SerializedName("extra_price") val extraPrice: Double = 0.0
)

// ── Admin Inventory ──
data class InventoryItem(
    val id: String,
    val name: String,
    @SerializedName("stock_count") val stockCount: Int = 0,
    @SerializedName("is_available") val isAvailable: Boolean = true
)

data class InventoryUpdateResponse(
    val id: String,
    val name: String,
    @SerializedName("stock_count") val stockCount: Int = 0,
    @SerializedName("is_available") val isAvailable: Boolean = true,
    @SerializedName("waitlist_notified") val waitlistNotified: Int = 0
)

// ── Admin Coupon ──
data class Coupon(
    val id: String,
    val code: String,
    @SerializedName("discount_type") val discountType: String,
    @SerializedName("discount_value") val discountValue: Double,
    @SerializedName("min_order_value") val minOrderValue: Double = 0.0,
    @SerializedName("max_uses") val maxUses: Int = 100,
    @SerializedName("used_count") val usedCount: Int = 0,
    @SerializedName("valid_from") val validFrom: String,
    @SerializedName("valid_until") val validUntil: String,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class CouponCreateRequest(
    val code: String,
    @SerializedName("discount_type") val discountType: String,
    @SerializedName("discount_value") val discountValue: Double,
    @SerializedName("min_order_value") val minOrderValue: Double = 0.0,
    @SerializedName("max_uses") val maxUses: Int = 100,
    @SerializedName("valid_from") val validFrom: String,
    @SerializedName("valid_until") val validUntil: String,
    @SerializedName("is_active") val isActive: Boolean = true
)

// ── Admin Delivery Config ──
data class DeliveryConfig(
    @SerializedName("bakery_lat") val bakeryLat: Double = 0.0,
    @SerializedName("bakery_lng") val bakeryLng: Double = 0.0,
    @SerializedName("free_delivery_radius_km") val freeDeliveryRadiusKm: Double = 0.0,
    @SerializedName("delivery_fee_default") val deliveryFeeDefault: Double = 0.0,
    @SerializedName("speed_min_per_km") val speedMinPerKm: Double = 0.0
)

data class DeliveryConfigUpdateRequest(
    @SerializedName("bakery_lat") val bakeryLat: Double? = null,
    @SerializedName("bakery_lng") val bakeryLng: Double? = null,
    @SerializedName("free_delivery_radius_km") val freeDeliveryRadiusKm: Double? = null,
    @SerializedName("delivery_fee_default") val deliveryFeeDefault: Double? = null,
    @SerializedName("speed_min_per_km") val speedMinPerKm: Double? = null
)

// ── Admin Category Create ──
data class CategoryCreateRequest(
    val name: String,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true
)

// ── Generic message response ──
data class MessageResponse(val message: String)
