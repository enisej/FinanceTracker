package com.example.financetracker.network

import retrofit2.http.Body
import retrofit2.http.POST

data class PostResponse(
    val id: Int,
    val title: String,
    val body: String,
    val userId: Int
)

interface ApiService {
    @POST("posts")
    suspend fun sendTransaction(
        @Body transaction: Map<String, @JvmSuppressWildcards Any>
    ): PostResponse
}