package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.*
import com.saibabui.openbake.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminProductEditUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val categories: List<Category> = emptyList(),
    val product: Product? = null,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class AdminProductEditViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AdminRepository()
    private val _uiState = MutableStateFlow(AdminProductEditUiState())
    val uiState: StateFlow<AdminProductEditUiState> = _uiState

    fun load(productId: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repo.getCategories().onSuccess { cats ->
                _uiState.value = _uiState.value.copy(categories = cats)
            }
            if (productId != null) {
                repo.getProducts().onSuccess { products ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        product = products.find { it.id == productId }
                    )
                }.onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = it.message)
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun saveProduct(
        productId: String?,
        name: String, description: String, price: Double, categoryId: String,
        images: List<String>, isAvailable: Boolean, isEggless: Boolean,
        customizable: Boolean, stockCount: Int, unlimitedStock: Boolean, variants: List<VariantRequest>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            val result = if (productId != null) {
                repo.updateProduct(productId, ProductUpdateRequest(
                    name = name, description = description, price = price,
                    images = images, isAvailable = isAvailable, isEgglessAvailable = isEggless,
                    customizable = customizable, stockCount = stockCount, unlimitedStock = unlimitedStock
                ))
            } else {
                repo.createProduct(ProductCreateRequest(
                    categoryId = categoryId, name = name, description = description,
                    price = price, images = images, isAvailable = isAvailable,
                    isEgglessAvailable = isEggless, customizable = customizable,
                    stockCount = stockCount, unlimitedStock = unlimitedStock, variants = variants
                ))
            }
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true) },
                onFailure = { _uiState.value = _uiState.value.copy(isSaving = false, error = it.message) }
            )
        }
    }
}
