package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.AdminOrderDetail
import com.saibabui.openbake.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminOrderDetailUiState(
    val isLoading: Boolean = false,
    val order: AdminOrderDetail? = null,
    val error: String? = null,
    val statusUpdateSuccess: Boolean = false
)

class AdminOrderDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AdminRepository()
    private val _uiState = MutableStateFlow(AdminOrderDetailUiState())
    val uiState: StateFlow<AdminOrderDetailUiState> = _uiState

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.getOrderDetail(orderId).fold(
                onSuccess = { _uiState.value = AdminOrderDetailUiState(order = it) },
                onFailure = { _uiState.value = AdminOrderDetailUiState(error = it.message) }
            )
        }
    }

    fun updateStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(error = null)
            repo.updateOrderStatus(orderId, newStatus).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(statusUpdateSuccess = true); loadOrder(orderId) },
                onFailure = { _uiState.value = _uiState.value.copy(error = it.message) }
            )
        }
    }
}
