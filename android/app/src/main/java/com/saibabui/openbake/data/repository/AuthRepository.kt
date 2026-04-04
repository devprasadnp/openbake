package com.saibabui.openbake.data.repository

import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.local.TokenManager
import com.saibabui.openbake.data.model.*
import kotlinx.coroutines.flow.first

class AuthRepository(private val tokenManager: TokenManager) {
    private val api = RetrofitClient.apiService

    suspend fun register(name: String, email: String, phone: String, password: String): Result<TokenResponse> {
        return try {
            val response = api.register(RegisterRequest(name, email, phone, password))
            if (response.isSuccessful) {
                val tokens = response.body()!!
                tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                Result.success(tokens)
            } else {
                val errorBody = response.errorBody()?.string()
                val msg = parseErrorDetail(errorBody) ?: "Registration failed: ${response.code()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<TokenResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val tokens = response.body()!!
                tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                Result.success(tokens)
            } else {
                val errorBody = response.errorBody()?.string()
                val msg = parseErrorDetail(errorBody) ?: "Login failed: ${response.code()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(): Result<User> {
        return try {
            val response = api.getProfile()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(name: String?, phone: String?): Result<User> {
        return try {
            val response = api.updateProfile(UpdateProfileRequest(name = name, phone = phone))
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val msg = parseErrorDetail(errorBody) ?: "Failed to update profile: ${response.code()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(filePart: okhttp3.MultipartBody.Part): Result<User> {
        return try {
            val response = api.uploadAvatar(filePart)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                val msg = parseErrorDetail(errorBody) ?: "Failed to upload avatar: ${response.code()}"
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try {
            val refreshToken = tokenManager.refreshToken.first()
            if (refreshToken != null) {
                // Revoke refresh token on server
                api.logout(RefreshTokenRequest(refreshToken))
            }
        } catch (_: Exception) {
            // Best-effort server revocation; always clear local tokens
        }
        tokenManager.clearTokens()
    }

    /**
     * Parse FastAPI error response {"detail": "..."} to extract the message.
     */
    private fun parseErrorDetail(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val regex = """"detail"\s*:\s*"([^"]+)"""".toRegex()
            regex.find(body)?.groupValues?.get(1)
        } catch (_: Exception) {
            null
        }
    }
}
