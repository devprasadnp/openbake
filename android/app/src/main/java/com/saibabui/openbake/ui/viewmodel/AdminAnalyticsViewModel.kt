package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.model.AnalyticsData
import com.saibabui.openbake.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminAnalyticsUiState(
    val isLoading: Boolean = false,
    val data: AnalyticsData? = null,
    val error: String? = null
)

class AdminAnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AdminRepository()
    private val _uiState = MutableStateFlow(AdminAnalyticsUiState())
    val uiState: StateFlow<AdminAnalyticsUiState> = _uiState

    init { loadAnalytics() }

    fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.getAnalytics().fold(
                onSuccess = { _uiState.value = AdminAnalyticsUiState(data = it) },
                onFailure = { _uiState.value = AdminAnalyticsUiState(error = it.message) }
            )
        }
    }
}
