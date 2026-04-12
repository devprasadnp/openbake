package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.Order
import com.saibabui.openbake.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminOrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val selectedFilter: String = "all",
    val error: String? = null
)

class AdminOrdersViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AdminRepository()
    private val _uiState = MutableStateFlow(AdminOrdersUiState())
    val uiState: StateFlow<AdminOrdersUiState> = _uiState

    init { loadOrders() }

    fun loadOrders(status: String = "all") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, selectedFilter = status)
            val filter = if (status == "all") null else status
            repo.getOrders(filter).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, orders = it) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
            )
        }
    }

    fun updateStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            repo.updateOrderStatus(orderId, newStatus).fold(
                onSuccess = { loadOrders(_uiState.value.selectedFilter) },
                onFailure = { _uiState.value = _uiState.value.copy(error = it.message) }
            )
        }
    }
}
