package com.saibabui.openbake.data.repository

import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.model.*

class OrderRepository {
    private val api = RetrofitClient.apiService

    /** Extract "detail" from a JSON error body, or return a fallback message. */
    private fun parseError(errorBody: okhttp3.ResponseBody?, fallback: String): String {
        val raw = errorBody?.string() ?: return fallback
        return try {
            org.json.JSONObject(raw).optString("detail", fallback)
        } catch (_: Exception) {
            fallback
        }
    }

    suspend fun createOrder(request: CreateOrderRequest): Result<Order> {
        return try {
            val response = api.createOrder(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response.errorBody(), "Failed to place order")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrders(): Result<List<Order>> {
        return try {
            val response = api.getOrders()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(parseError(response.errorBody(), "Failed to fetch orders")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderById(id: String): Result<Order> {
        return try {
            val response = api.getOrderById(id)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response.errorBody(), "Failed to fetch order")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(id: String): Result<Order> {
        return try {
            val response = api.cancelOrder(id)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response.errorBody(), "Failed to cancel order")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAddresses(): Result<List<Address>> {
        return try {
            val response = api.getAddresses()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception(parseError(response.errorBody(), "Failed to fetch addresses")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Delivery ──
    suspend fun getDeliveryEstimate(lat: Double, lng: Double): Result<DeliveryEstimate> {
        return try {
            val response = api.getDeliveryEstimate(lat, lng)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response.errorBody(), "Delivery estimate failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Payment ──
    suspend fun createPaymentOrder(orderId: String): Result<RazorpayOrderResponse> {
        return try {
            val response = api.createPaymentOrder(CreatePaymentOrderRequest(orderId))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response.errorBody(), "Payment order creation failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyPayment(request: VerifyPaymentRequest): Result<PaymentVerifyResponse> {
        return try {
            val response = api.verifyPayment(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response.errorBody(), "Payment verification failed")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Waitlist ──
    suspend fun joinWaitlist(productId: String): Result<StockWaitlistResponse> {
        return try {
            val response = api.joinWaitlist(productId)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(parseError(response.errorBody(), "Failed to join waitlist")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveWaitlist(productId: String): Result<Unit> {
        return try {
            val response = api.leaveWaitlist(productId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseError(response.errorBody(), "Failed to leave waitlist")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
