package com.saibabui.openbake.data.api

import com.saibabui.openbake.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<TokenResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): Response<TokenResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<TokenResponse>

    // Categories
    @GET("categories")
    suspend fun getCategories(): Response<List<Category>>

    // Products
    @GET("products")
    suspend fun getProducts(
        @Query("category_id") categoryId: String? = null,
        @Query("search") search: String? = null,
        @Query("eggless_only") egglessOnly: Boolean? = null,
        @Query("sort_by") sortBy: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<PaginatedProductResponse>

    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: String): Response<Product>

    @GET("products/{id}/reviews")
    suspend fun getProductReviews(@Path("id") id: String): Response<List<Review>>

    // Orders
    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<Order>

    @GET("orders")
    suspend fun getOrders(): Response<List<Order>>

    @GET("orders/{id}")
    suspend fun getOrderById(@Path("id") id: String): Response<Order>

    @PATCH("orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: String): Response<Order>

    // Profile
    @GET("profile")
    suspend fun getProfile(): Response<User>

    @PATCH("profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<User>

    @Multipart
    @POST("profile/avatar")
    suspend fun uploadAvatar(@Part file: okhttp3.MultipartBody.Part): Response<User>

    // Addresses
    @GET("addresses")
    suspend fun getAddresses(): Response<List<Address>>

    @POST("addresses")
    suspend fun addAddress(@Body request: AddressRequest): Response<Address>

    @PATCH("addresses/{id}")
    suspend fun updateAddress(@Path("id") id: String, @Body request: AddressRequest): Response<Address>

    @DELETE("addresses/{id}")
    suspend fun deleteAddress(@Path("id") id: String): Response<Unit>

    // Wishlist
    @GET("wishlist")
    suspend fun getWishlist(): Response<List<WishlistItem>>

    @POST("wishlist/{productId}")
    suspend fun addToWishlist(@Path("productId") productId: String): Response<Unit>

    @DELETE("wishlist/{productId}")
    suspend fun removeFromWishlist(@Path("productId") productId: String): Response<Unit>

    // Reviews
    @POST("reviews")
    suspend fun addReview(@Body request: ReviewRequest): Response<Review>

    // Delivery Estimate
    @GET("delivery/estimate")
    suspend fun getDeliveryEstimate(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double
    ): Response<DeliveryEstimate>

    // Payments
    @POST("payments/create-order")
    suspend fun createPaymentOrder(@Body request: CreatePaymentOrderRequest): Response<RazorpayOrderResponse>

    @POST("payments/verify")
    suspend fun verifyPayment(@Body request: VerifyPaymentRequest): Response<PaymentVerifyResponse>

    // Coupons
    @POST("coupons/apply")
    suspend fun applyCoupon(@Body request: CouponApplyRequest): Response<CouponApplyResponse>

    // Stock Waitlist
    @POST("waitlist/{productId}")
    suspend fun joinWaitlist(@Path("productId") productId: String): Response<StockWaitlistResponse>

    @DELETE("waitlist/{productId}")
    suspend fun leaveWaitlist(@Path("productId") productId: String): Response<Unit>

    @GET("waitlist")
    suspend fun getWaitlist(): Response<List<StockWaitlistResponse>>

    // OTP Auth
    @POST("auth/otp/send")
    suspend fun sendOtp(@Body request: OtpSendRequest): Response<OtpSendResponse>

    @POST("auth/otp/verify")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): Response<TokenResponse>

    // Auth — logout with refresh token revocation
    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest): Response<Unit>

    // ── Admin ──

    @GET("admin/dashboard")
    suspend fun getAdminDashboard(): Response<DashboardStats>

    @GET("admin/analytics")
    suspend fun getAdminAnalytics(): Response<AnalyticsData>

    @GET("admin/orders")
    suspend fun getAdminOrders(@Query("status_filter") status: String? = null): Response<List<Order>>

    @GET("admin/orders/{id}")
    suspend fun getAdminOrderDetail(@Path("id") id: String): Response<AdminOrderDetail>

    @PATCH("admin/orders/{id}")
    suspend fun updateOrderStatus(@Path("id") id: String, @Body body: OrderStatusUpdateRequest): Response<Order>

    @GET("admin/products")
    suspend fun getAdminProducts(): Response<List<Product>>

    @POST("admin/products")
    suspend fun createProduct(@Body body: ProductCreateRequest): Response<Product>

    @PATCH("admin/products/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Body body: ProductUpdateRequest): Response<Product>

    @DELETE("admin/products/{id}")
    suspend fun deleteProduct(@Path("id") id: String): Response<MessageResponse>

    @GET("admin/inventory")
    suspend fun getAdminInventory(@Query("threshold") threshold: Int = 10): Response<List<InventoryItem>>

    @PATCH("admin/inventory/{id}")
    suspend fun updateInventoryStock(@Path("id") id: String, @Query("stock_count") stockCount: Int): Response<InventoryUpdateResponse>

    @GET("admin/categories")
    suspend fun getAdminCategories(): Response<List<Category>>

    @POST("admin/categories")
    suspend fun createCategory(@Body body: CategoryCreateRequest): Response<Category>

    @PATCH("admin/categories/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body body: CategoryCreateRequest): Response<Category>

    @DELETE("admin/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<MessageResponse>

    @GET("admin/coupons")
    suspend fun getAdminCoupons(): Response<List<Coupon>>

    @POST("admin/coupons")
    suspend fun createCoupon(@Body body: CouponCreateRequest): Response<Coupon>

    @PATCH("admin/coupons/{id}")
    suspend fun updateCoupon(@Path("id") id: String, @Body body: CouponCreateRequest): Response<Coupon>

    @GET("admin/delivery-config")
    suspend fun getDeliveryConfig(): Response<DeliveryConfig>

    @PATCH("admin/delivery-config")
    suspend fun updateDeliveryConfig(@Body body: DeliveryConfigUpdateRequest): Response<DeliveryConfig>
}
