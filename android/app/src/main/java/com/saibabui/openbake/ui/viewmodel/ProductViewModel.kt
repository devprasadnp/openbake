package com.saibabui.openbake.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.Product
import com.saibabui.openbake.data.model.Review
import com.saibabui.openbake.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProductListUiState(
    val isLoading: Boolean = true,
    val products: List<Product> = emptyList(),
    val error: String? = null
)

data class ProductDetailUiState(
    val isLoading: Boolean = true,
    val product: Product? = null,
    val reviews: List<Review> = emptyList(),
    val error: String? = null
)

class ProductViewModel : ViewModel() {
    private val productRepo = ProductRepository()

    private val _listState = MutableStateFlow(ProductListUiState())
    val listState: StateFlow<ProductListUiState> = _listState

    private val _detailState = MutableStateFlow(ProductDetailUiState())
    val detailState: StateFlow<ProductDetailUiState> = _detailState

    fun loadProducts(
        categoryId: String? = null,
        search: String? = null,
        egglessOnly: Boolean? = null,
        sortBy: String? = null
    ) {
        viewModelScope.launch {
            _listState.value = ProductListUiState(isLoading = true)
            val result = productRepo.getProducts(
                categoryId = categoryId,
                search = search,
                isEggless = egglessOnly,
                sortBy = sortBy,
                pageSize = 50
            )
            result.fold(
                onSuccess = { products ->
                    _listState.value = ProductListUiState(isLoading = false, products = products)
                },
                onFailure = { e ->
                    _listState.value = ProductListUiState(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun loadProductDetail(productId: String) {
        viewModelScope.launch {
            _detailState.value = ProductDetailUiState(isLoading = true)
            val productResult = productRepo.getProductById(productId)
            val reviewsResult = productRepo.getProductReviews(productId)

            productResult.fold(
                onSuccess = { product ->
                    _detailState.value = ProductDetailUiState(
                        isLoading = false,
                        product = product,
                        reviews = reviewsResult.getOrDefault(emptyList())
                    )
                },
                onFailure = { e ->
                    _detailState.value = ProductDetailUiState(isLoading = false, error = e.message)
                }
            )
        }
    }
}
