package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.Product
import com.saibabui.openbake.data.model.ProductUpdateRequest
import com.saibabui.openbake.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminProductsUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val error: String? = null
)

class AdminProductsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AdminRepository()
    private val _uiState = MutableStateFlow(AdminProductsUiState())
    val uiState: StateFlow<AdminProductsUiState> = _uiState

    init { loadProducts() }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.getProducts().fold(
                onSuccess = { _uiState.value = AdminProductsUiState(products = it) },
                onFailure = { _uiState.value = AdminProductsUiState(error = it.message) }
            )
        }
    }

    fun toggleAvailability(product: Product) {
        viewModelScope.launch {
            repo.updateProduct(product.id, ProductUpdateRequest(isAvailable = !product.isAvailable)).fold(
                onSuccess = { loadProducts() },
                onFailure = { _uiState.value = _uiState.value.copy(error = it.message) }
            )
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repo.deleteProduct(productId).fold(
                onSuccess = { loadProducts() },
                onFailure = { _uiState.value = _uiState.value.copy(error = it.message) }
            )
        }
    }
}
