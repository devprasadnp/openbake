package com.saibabui.openbake.data.api

import com.saibabui.openbake.data.local.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://0203-2402-e280-213a-15c-15f9-99ea-1df1-1ba4.ngrok-free.app/api/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private var tokenManager: TokenManager? = null

    fun init(tokenManager: TokenManager) {
        this.tokenManager = tokenManager
    }

    private val authInterceptor = Interceptor { chain ->
        val token = tokenManager?.let {
            runBlocking { it.accessToken.first() }
        }
        val request = chain.request().newBuilder().apply {
            token?.let { addHeader("Authorization", "Bearer $it") }
        }.build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
