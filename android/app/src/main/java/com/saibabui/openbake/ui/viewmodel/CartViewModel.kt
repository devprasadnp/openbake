package com.saibabui.openbake.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.saibabui.openbake.data.model.CartItem
import com.saibabui.openbake.data.model.Product
import com.saibabui.openbake.data.model.ProductVariant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CartViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items

    val subtotal: Double
        get() = _items.value.sumOf { it.totalPrice }

    val deliveryFee: Double
        get() = if (_items.value.isEmpty()) 0.0 else 40.0

    val total: Double
        get() = subtotal + deliveryFee

    val totalItems: Int
        get() = _items.value.sumOf { it.quantity }

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
    }
}
