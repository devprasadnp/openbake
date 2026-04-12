package com.saibabui.openbake.data.repository

import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.model.*

class AdminRepository {
    private val api get() = RetrofitClient.apiService

    suspend fun getDashboard(): Result<DashboardStats> = apiCall { api.getAdminDashboard() }
    suspend fun getAnalytics(): Result<AnalyticsData> = apiCall { api.getAdminAnalytics() }

    // Orders
    suspend fun getOrders(status: String? = null): Result<List<Order>> = apiCall { api.getAdminOrders(status) }
    suspend fun getOrderDetail(id: String): Result<AdminOrderDetail> = apiCall { api.getAdminOrderDetail(id) }
    suspend fun updateOrderStatus(id: String, status: String): Result<Order> =
        apiCall { api.updateOrderStatus(id, OrderStatusUpdateRequest(status)) }

    // Products
    suspend fun getProducts(): Result<List<Product>> = apiCall { api.getAdminProducts() }
    suspend fun createProduct(request: ProductCreateRequest): Result<Product> = apiCall { api.createProduct(request) }
    suspend fun updateProduct(id: String, request: ProductUpdateRequest): Result<Product> = apiCall { api.updateProduct(id, request) }
    suspend fun deleteProduct(id: String): Result<MessageResponse> = apiCall { api.deleteProduct(id) }

    // Inventory
    suspend fun getInventory(threshold: Int = 10): Result<List<InventoryItem>> = apiCall { api.getAdminInventory(threshold) }
    suspend fun updateStock(id: String, stockCount: Int): Result<InventoryUpdateResponse> = apiCall { api.updateInventoryStock(id, stockCount) }

    // Categories
    suspend fun getCategories(): Result<List<Category>> = apiCall { api.getAdminCategories() }
    suspend fun createCategory(request: CategoryCreateRequest): Result<Category> = apiCall { api.createCategory(request) }
    suspend fun updateCategory(id: String, request: CategoryCreateRequest): Result<Category> = apiCall { api.updateCategory(id, request) }
    suspend fun deleteCategory(id: String): Result<MessageResponse> = apiCall { api.deleteCategory(id) }

    // Coupons
    suspend fun getCoupons(): Result<List<Coupon>> = apiCall { api.getAdminCoupons() }
    suspend fun createCoupon(request: CouponCreateRequest): Result<Coupon> = apiCall { api.createCoupon(request) }
    suspend fun updateCoupon(id: String, request: CouponCreateRequest): Result<Coupon> = apiCall { api.updateCoupon(id, request) }

    // Delivery Config
    suspend fun getDeliveryConfig(): Result<DeliveryConfig> = apiCall { api.getDeliveryConfig() }
    suspend fun updateDeliveryConfig(request: DeliveryConfigUpdateRequest): Result<DeliveryConfig> = apiCall { api.updateDeliveryConfig(request) }

    private suspend fun <T> apiCall(call: suspend () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Empty response"))
            } else {
                Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
