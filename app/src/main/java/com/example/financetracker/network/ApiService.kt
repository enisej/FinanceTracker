package com.example.financetracker.network

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("posts")
    suspend fun sendTransaction(@Body transaction: Map<String, Any>): Map<String, Any>
}