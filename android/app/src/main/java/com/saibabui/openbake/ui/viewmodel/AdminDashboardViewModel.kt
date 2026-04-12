package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.DashboardStats
import com.saibabui.openbake.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val stats: DashboardStats? = null,
    val error: String? = null
)

class AdminDashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AdminRepository()
    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.getDashboard().fold(
                onSuccess = { _uiState.value = AdminDashboardUiState(stats = it) },
                onFailure = { _uiState.value = AdminDashboardUiState(error = it.message) }
            )
        }
    }
}
