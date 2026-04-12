package com.saibabui.openbake.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.Order
import com.saibabui.openbake.data.model.CreateOrderRequest
import com.saibabui.openbake.data.model.OrderItemRequest
import com.saibabui.openbake.data.model.CartItem
import com.saibabui.openbake.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class OrderListUiState(
    val isLoading: Boolean = true,
    val orders: List<Order> = emptyList(),
    val error: String? = null
)

data class OrderDetailUiState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val error: String? = null
)

class OrderViewModel : ViewModel() {
    private val orderRepo = OrderRepository()

    private val _listState = MutableStateFlow(OrderListUiState())
    val listState: StateFlow<OrderListUiState> = _listState

    private val _detailState = MutableStateFlow(OrderDetailUiState())
    val detailState: StateFlow<OrderDetailUiState> = _detailState

    private val _placingOrder = MutableStateFlow(false)
    val placingOrder: StateFlow<Boolean> = _placingOrder

    private val _placedOrder = MutableStateFlow<Order?>(null)
    val placedOrder: StateFlow<Order?> = _placedOrder

    private val _orderError = MutableStateFlow<String?>(null)
    val orderError: StateFlow<String?> = _orderError

    fun loadOrders() {
        viewModelScope.launch {
            _listState.value = OrderListUiState(isLoading = true)
            val result = orderRepo.getOrders()
            result.fold(
                onSuccess = { orders ->
                    _listState.value = OrderListUiState(isLoading = false, orders = orders)
                },
                onFailure = { e ->
                    _listState.value = OrderListUiState(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun loadOrderDetail(orderId: String) {
        viewModelScope.launch {
            _detailState.value = OrderDetailUiState(isLoading = true)
            val result = orderRepo.getOrderById(orderId)
            result.fold(
                onSuccess = { order ->
                    _detailState.value = OrderDetailUiState(isLoading = false, order = order)
                },
                onFailure = { e ->
                    _detailState.value = OrderDetailUiState(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun placeOrder(
        cartItems: List<CartItem>,
        paymentMethod: String = "cod",
        orderType: String = "delivery",
        addressId: String? = null,
        couponCode: String? = null,
        scheduledDate: String? = null,
        timeSlot: String? = null,
        specialNote: String? = null,
        idempotencyKey: String = UUID.randomUUID().toString()
    ) {
        viewModelScope.launch {
            _placingOrder.value = true
            _orderError.value = null

            val items = cartItems.map { item ->
                OrderItemRequest(
                    productId = item.product.id,
                    quantity = item.quantity,
                    unitPrice = item.itemPrice
                )
            }

            val request = CreateOrderRequest(
                addressId = if (orderType == "delivery") addressId else null,
                orderType = orderType,
                items = items,
                paymentMethod = paymentMethod,
                couponCode = couponCode,
                scheduledDate = scheduledDate,
                timeSlot = timeSlot,
                specialNote = specialNote,
                idempotencyKey = idempotencyKey
            )

            val result = orderRepo.createOrder(request)
            result.fold(
                onSuccess = { order ->
                    _placedOrder.value = order
                    _placingOrder.value = false
                },
                onFailure = { e ->
                    // Sanitize error message for display
                    val raw = e.message ?: "Something went wrong"
                    _orderError.value = when {
                        raw.contains("address", ignoreCase = true) -> "Invalid address. Please select a valid delivery address."
                        raw.contains("stock", ignoreCase = true) || raw.contains("inventory", ignoreCase = true) -> "Some items are out of stock. Please update your cart."
                        raw.contains("timeout", ignoreCase = true) || raw.contains("connect", ignoreCase = true) -> "Network error. Please check your connection and try again."
                        else -> "Something went wrong while placing your order. Please try again."
                    }
                    _placingOrder.value = false
                }
            )
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoading = true)
            val result = orderRepo.cancelOrder(orderId)
            result.fold(
                onSuccess = { order ->
                    _detailState.value = OrderDetailUiState(isLoading = false, order = order)
                },
                onFailure = { e ->
                    _detailState.value = _detailState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun clearPlacedOrder() {
        _placedOrder.value = null
    }

    fun clearOrderError() {
        _orderError.value = null
    }
}
