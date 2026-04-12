package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.InventoryItem
import com.saibabui.openbake.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminInventoryUiState(
    val isLoading: Boolean = false,
    val items: List<InventoryItem> = emptyList(),
    val error: String? = null
)

class AdminInventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AdminRepository()
    private val _uiState = MutableStateFlow(AdminInventoryUiState())
    val uiState: StateFlow<AdminInventoryUiState> = _uiState

    init { loadInventory() }

    fun loadInventory(threshold: Int = 9999) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.getInventory(threshold).fold(
                onSuccess = { _uiState.value = AdminInventoryUiState(items = it) },
                onFailure = { _uiState.value = AdminInventoryUiState(error = it.message) }
            )
        }
    }

    fun updateStock(productId: String, newCount: Int) {
        viewModelScope.launch {
            repo.updateStock(productId, newCount).fold(
                onSuccess = { loadInventory() },
                onFailure = { _uiState.value = _uiState.value.copy(error = it.message) }
            )
        }
    }
}
