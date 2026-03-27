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
    val role: String = "customer"
)

data class UpdateProfileRequest(
    val name: String? = null,
    val phone: String? = null
)

data class Address(
    val id: String,
    val label: String,
    @SerializedName("full_address") val fullAddress: String,
    val city: String,
    val pincode: String,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerializedName("is_default") val isDefault: Boolean = false
)

data class AddressRequest(
    val label: String,
    @SerializedName("full_address") val fullAddress: String,
    val city: String,
    val pincode: String,
    @SerializedName("is_default") val isDefault: Boolean = false
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
    @SerializedName("special_note") val specialNote: String? = null
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
    @SerializedName("scheduled_date") val scheduledDate: String? = null,
    @SerializedName("time_slot") val timeSlot: String? = null,
    @SerializedName("special_note") val specialNote: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String? = null
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
