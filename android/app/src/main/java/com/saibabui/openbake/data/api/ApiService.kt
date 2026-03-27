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
    ): Response<List<Product>>

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

    // Addresses
    @GET("addresses")
    suspend fun getAddresses(): Response<List<Address>>

    @POST("addresses")
    suspend fun addAddress(@Body request: AddressRequest): Response<Address>

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
}
