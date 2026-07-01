package com.aryan.stressbridge

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*

import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*

class ServerUploader(val username: String, val password: String) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 5000
        }
    }

    suspend fun uploadRecord(jsonPayload: String) = withContext(Dispatchers.IO) {
        try {
            // REPLACE THIS IP WITH YOUR CURRENT 'ipconfig' IP
            val response = client.post("http://10.145.124.88:5000/data") {
                contentType(ContentType.Application.Json)
                setBody(jsonPayload)
            }
            Log.d("SERVER_LOG", "Data sent! Response: ${response.status}")
        } catch (e: Exception) {
            Log.e("SERVER_LOG", "CRITICAL UPLOAD ERROR: ${e.message}")
        }
    }
}