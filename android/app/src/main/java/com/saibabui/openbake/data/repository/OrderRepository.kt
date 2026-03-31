package com.saibabui.openbake.data.repository

import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.model.*

class OrderRepository {
    private val api = RetrofitClient.apiService

    suspend fun createOrder(request: CreateOrderRequest): Result<Order> {
        return try {
            val response = api.createOrder(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val detail = response.errorBody()?.string() ?: "Failed: ${response.code()}"
                Result.failure(Exception(detail))
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
                Result.failure(Exception("Failed to fetch orders: ${response.code()}"))
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
                Result.failure(Exception("Failed to fetch order: ${response.code()}"))
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
                Result.failure(Exception("Failed to cancel order: ${response.code()}"))
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
                Result.failure(Exception("Failed to fetch addresses: ${response.code()}"))
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
                Result.failure(Exception("Delivery estimate failed: ${response.code()}"))
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
                Result.failure(Exception("Payment order failed: ${response.code()}"))
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
                Result.failure(Exception("Payment verify failed: ${response.code()}"))
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
                Result.failure(Exception("Waitlist join failed: ${response.code()}"))
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
                Result.failure(Exception("Waitlist leave failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
