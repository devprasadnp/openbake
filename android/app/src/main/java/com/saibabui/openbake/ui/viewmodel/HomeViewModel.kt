package com.saibabui.openbake.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.Category
import com.saibabui.openbake.data.model.Product
import com.saibabui.openbake.data.repository.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val bestsellers: List<Product> = emptyList(),
    val error: String? = null
)

class HomeViewModel : ViewModel() {
    private val productRepo = ProductRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)
            val catResult = productRepo.getCategories()
            val prodResult = productRepo.getProducts(pageSize = 8)

            _uiState.value = HomeUiState(
                isLoading = false,
                categories = catResult.getOrDefault(emptyList()),
                bestsellers = prodResult.getOrDefault(emptyList()),
                error = catResult.exceptionOrNull()?.message ?: prodResult.exceptionOrNull()?.message
            )
        }
    }
}
