package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.DeliveryConfig
import com.saibabui.openbake.data.model.DeliveryConfigUpdateRequest
import com.saibabui.openbake.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminSettingsUiState(
    val isLoading: Boolean = false,
    val config: DeliveryConfig? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class AdminSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AdminRepository()
    private val _uiState = MutableStateFlow(AdminSettingsUiState())
    val uiState: StateFlow<AdminSettingsUiState> = _uiState

    init { loadConfig() }

    fun loadConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.getDeliveryConfig().fold(
                onSuccess = { _uiState.value = AdminSettingsUiState(config = it) },
                onFailure = { _uiState.value = AdminSettingsUiState(error = it.message) }
            )
        }
    }

    fun saveConfig(request: DeliveryConfigUpdateRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)
            repo.updateDeliveryConfig(request).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isSaving = false, config = it, saveSuccess = true) },
                onFailure = { _uiState.value = _uiState.value.copy(isSaving = false, error = it.message) }
            )
        }
    }

    fun resetSaveState() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}
