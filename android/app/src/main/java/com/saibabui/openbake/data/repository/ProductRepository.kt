package com.saibabui.openbake.data.repository

import com.saibabui.openbake.data.api.RetrofitClient
import com.saibabui.openbake.data.model.*

class ProductRepository {
    private val api = RetrofitClient.apiService

    suspend fun getCategories(): Result<List<Category>> {
        return try {
            val response = api.getCategories()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch categories: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProducts(
        categoryId: String? = null,
        search: String? = null,
        isEggless: Boolean? = null,
        sortBy: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<List<Product>> {
        return try {
            val response = api.getProducts(categoryId, search, isEggless, sortBy, page, pageSize)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch products: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductById(id: String): Result<Product> {
        return try {
            val response = api.getProductById(id)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch product: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductReviews(productId: String): Result<List<Review>> {
        return try {
            val response = api.getProductReviews(productId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch reviews: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
