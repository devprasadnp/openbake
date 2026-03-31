package com.saibabui.openbake.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.CartItem
import com.saibabui.openbake.data.model.DeliveryEstimate
import com.saibabui.openbake.data.model.Product
import com.saibabui.openbake.data.model.ProductVariant
import com.saibabui.openbake.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {
    private val orderRepo = OrderRepository()

    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items

    private val _deliveryEstimate = MutableStateFlow<DeliveryEstimate?>(null)
    val deliveryEstimate: StateFlow<DeliveryEstimate?> = _deliveryEstimate

    private val _deliveryLoading = MutableStateFlow(false)
    val deliveryLoading: StateFlow<Boolean> = _deliveryLoading

    val subtotal: Double
        get() = _items.value.sumOf { it.totalPrice }

    val deliveryFee: Double
        get() = _deliveryEstimate.value?.deliveryFee ?: if (_items.value.isEmpty()) 0.0 else 40.0

    val estimatedMinutes: Int?
        get() = _deliveryEstimate.value?.estimatedMinutes

    val isDeliverable: Boolean
        get() = _deliveryEstimate.value?.isDeliverable ?: true

    val total: Double
        get() = subtotal + deliveryFee

    val totalItems: Int
        get() = _items.value.sumOf { it.quantity }

    /**
     * Fetch delivery estimate from the API using the given coordinates.
     */
    fun fetchDeliveryEstimate(lat: Double, lng: Double) {
        viewModelScope.launch {
            _deliveryLoading.value = true
            val result = orderRepo.getDeliveryEstimate(lat, lng)
            result.fold(
                onSuccess = { estimate ->
                    _deliveryEstimate.value = estimate
                },
                onFailure = {
                    // If the API fails, fall back to default ₹40
                    _deliveryEstimate.value = null
                }
            )
            _deliveryLoading.value = false
        }
    }

    fun addItem(
        product: Product,
        quantity: Int = 1,
        variant: ProductVariant? = null,
        isEggless: Boolean = false,
        cakeMessage: String? = null
    ) {
        val current = _items.value.toMutableList()
        val existingIdx = current.indexOfFirst {
            it.product.id == product.id &&
                    it.selectedVariant?.id == variant?.id &&
                    it.isEggless == isEggless
        }
        if (existingIdx >= 0) {
            val existing = current[existingIdx]
            current[existingIdx] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            current.add(
                CartItem(
                    product = product,
                    quantity = quantity,
                    selectedVariant = variant,
                    isEggless = isEggless,
                    cakeMessage = cakeMessage
                )
            )
        }
        _items.value = current
    }

    fun updateQuantity(index: Int, quantity: Int) {
        val current = _items.value.toMutableList()
        if (index in current.indices) {
            if (quantity <= 0) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = quantity)
            }
            _items.value = current
        }
    }

    fun removeItem(index: Int) {
        val current = _items.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _items.value = current
        }
    }

    fun clearCart() {
        _items.value = emptyList()
        _deliveryEstimate.value = null
    }
}
