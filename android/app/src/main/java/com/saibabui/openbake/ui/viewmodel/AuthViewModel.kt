package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.local.TokenManager
import com.saibabui.openbake.data.model.User
import com.saibabui.openbake.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenManager = TokenManager(application)
    private val authRepo = AuthRepository(tokenManager)

    init {
        RetrofitClient.init(tokenManager)
    }

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun checkAuthState() {
        viewModelScope.launch {
            val token = tokenManager.accessToken.first()
            if (token != null) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val result = authRepo.getProfile()
                result.fold(
                    onSuccess = { user ->
                        _uiState.value = AuthUiState(isLoggedIn = true, user = user)
                    },
                    onFailure = {
                        _uiState.value = AuthUiState(isLoggedIn = false)
                    }
                )
            } else {
                _uiState.value = AuthUiState(isLoggedIn = false)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepo.login(email, password)
            result.fold(
                onSuccess = {
                    val profileResult = authRepo.getProfile()
                    profileResult.fold(
                        onSuccess = { user ->
                            _uiState.value = AuthUiState(isLoggedIn = true, user = user)
                        },
                        onFailure = {
                            _uiState.value = AuthUiState(isLoggedIn = true)
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun register(name: String, email: String, phone: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepo.register(name, email, phone, password)
            result.fold(
                onSuccess = {
                    val profileResult = authRepo.getProfile()
                    profileResult.fold(
                        onSuccess = { user ->
                            _uiState.value = AuthUiState(isLoggedIn = true, user = user)
                        },
                        onFailure = {
                            _uiState.value = AuthUiState(isLoggedIn = true)
                        }
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            _uiState.value = AuthUiState(isLoggedIn = false)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
