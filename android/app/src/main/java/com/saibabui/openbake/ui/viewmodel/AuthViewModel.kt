package com.saibabui.openbake.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.local.TokenManager
import com.saibabui.openbake.data.model.OtpSendRequest
import com.saibabui.openbake.data.model.OtpVerifyRequest
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
    val error: String? = null,
    val updateSuccess: Boolean = false,
    val otpSent: Boolean = false,
    val otpSending: Boolean = false
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
            _uiState.value = AuthUiState(isLoggedIn = false)
            runCatching { authRepo.logout() }
        }
    }

    fun updateProfile(name: String?, phone: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, updateSuccess = false)
            val result = authRepo.updateProfile(name, phone)
            result.fold(
                onSuccess = { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = updatedUser,
                        updateSuccess = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to update profile"
                    )
                }
            )
        }
    }

    fun uploadAvatar(filePart: okhttp3.MultipartBody.Part) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepo.uploadAvatar(filePart)
            result.fold(
                onSuccess = { updatedUser ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = updatedUser
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to upload avatar"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetUpdateSuccess() {
        _uiState.value = _uiState.value.copy(updateSuccess = false)
    }

    fun sendOtp(phone: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(otpSending = true, error = null)
            try {
                val resp = RetrofitClient.apiService.sendOtp(OtpSendRequest(phone))
                if (resp.isSuccessful) {
                    _uiState.value = _uiState.value.copy(otpSending = false, otpSent = true)
                } else {
                    _uiState.value = _uiState.value.copy(otpSending = false, error = "Failed to send OTP")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(otpSending = false, error = e.message ?: "Network error")
            }
        }
    }

    fun verifyOtp(phone: String, otp: String, name: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val resp = RetrofitClient.apiService.verifyOtp(OtpVerifyRequest(phone, otp, name))
                if (resp.isSuccessful) {
                    val tokens = resp.body()
                    if (tokens != null) {
                        tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                        val profileResult = authRepo.getProfile()
                        profileResult.fold(
                            onSuccess = { user ->
                                _uiState.value = AuthUiState(isLoggedIn = true, user = user)
                            },
                            onFailure = {
                                _uiState.value = AuthUiState(isLoggedIn = true)
                            }
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Invalid OTP. Please try again.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Verification failed")
            }
        }
    }

    fun resetOtpState() {
        _uiState.value = _uiState.value.copy(otpSent = false, otpSending = false)
    }
}
