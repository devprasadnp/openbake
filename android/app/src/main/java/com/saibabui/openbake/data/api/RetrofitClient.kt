package com.saibabui.openbake.data.api

import com.saibabui.openbake.BuildConfig
import com.saibabui.openbake.data.local.TokenManager
import com.saibabui.openbake.data.model.RefreshTokenRequest
import com.saibabui.openbake.data.model.TokenResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val BASE_URL: String
        get() = BuildConfig.BASE_URL

    fun getBaseUrl(): String = BASE_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    }

    private var tokenManager: TokenManager? = null

    fun init(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
    }

    /**
     * Interceptor that attaches the current access token to every request.
     * Uses runBlocking only to read the cached DataStore value (fast path).
     */
    private val authInterceptor = Interceptor { chain ->
        val token = tokenManager?.let {
            runBlocking { it.accessToken.first() }
        }
        val request = chain.request().newBuilder().apply {
            token?.let { addHeader("Authorization", "Bearer $it") }
        }.build()
        chain.proceed(request)
    }

    /**
     * OkHttp Authenticator that fires when a 401 is received.
     * It reads the stored refresh token, calls /api/auth/refresh,
     * saves the new token pair, and retries the original request.
     * If refresh also fails, it clears tokens (forces re-login).
     */
    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // Avoid infinite refresh loops — if we already tried refreshing, give up
            if (response.request.header("X-Retry-After-Refresh") != null) {
                return null
            }

            val tm = tokenManager ?: return null
            val refreshToken = runBlocking { tm.refreshToken.first() } ?: return null

            // Build a one-off Retrofit instance (without the authenticator) to call refresh
            val refreshClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val refreshApi = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(refreshClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)

            return try {
                val refreshResponse = runBlocking {
                    refreshApi.refreshToken(RefreshTokenRequest(refreshToken))
                }
                if (refreshResponse.isSuccessful) {
                    val tokens = refreshResponse.body()!!
                    runBlocking { tm.saveTokens(tokens.accessToken, tokens.refreshToken) }

                    // Retry the original request with the new access token
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${tokens.accessToken}")
                        .header("X-Retry-After-Refresh", "true")
                        .build()
                } else {
                    // Refresh failed — clear tokens and force re-login
                    runBlocking { tm.clearTokens() }
                    null
                }
            } catch (_: Exception) {
                runBlocking { tm.clearTokens() }
                null
            }
        }
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
