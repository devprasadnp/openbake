package com.saibabui.openbake.data.repository

import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.local.TokenManager
import com.saibabui.openbake.data.model.*

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
                Result.failure(Exception("Registration failed: ${response.code()}"))
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
                Result.failure(Exception("Login failed: ${response.code()}"))
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
                Result.failure(Exception("Failed to update profile: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clearTokens()
    }
}
