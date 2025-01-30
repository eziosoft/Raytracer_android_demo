package com.example.fps_raytrace

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

const val SYSTEM_PROMPT = "Your name is Emma."

data class OllamaRequest(
    val model: String,   // e.g., "mistral"
    val prompt: String,  // User's question
    val stream: Boolean = false,
    val system: String = SYSTEM_PROMPT
)

data class OllamaResponse(
    val response: String // The AI-generated response
)

interface OllamaApiService {
    @Headers("Content-Type: application/json")
    @POST("api/generate")
    suspend fun getResponse(@Body request: OllamaRequest): Response<OllamaResponse>
}


object OllamaApi {
    private val instance: OllamaApiService by lazy {
        RetrofitClient.instance
    }

    suspend fun getResponse(request: OllamaRequest): Response<OllamaResponse> {
        return instance.getResponse(request)
    }
}




