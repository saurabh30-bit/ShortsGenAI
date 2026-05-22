package com.aistudio.shortsgen.network

import com.aistudio.shortsgen.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class FastApiClient {
    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun sanitizeUrl(url: String): String {
        var cleaned = url.trim()
        if (cleaned.isEmpty()) return ""
        if (!cleaned.startsWith("http://") && !cleaned.startsWith("https://")) {
            cleaned = "http://$cleaned"
        }
        while (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length - 1)
        }
        return cleaned
    }

    suspend fun triggerVideoGeneration(
        settings: AppSettings,
        prompt: String,
        script: String,
        hashtags: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val sanitizedBase = sanitizeUrl(settings.fastapiUrl)
        if (sanitizedBase.isEmpty()) {
            return@withContext Result.failure(Exception("FastAPI URL is empty or invalid"))
        }
        val targetUrl = "$sanitizedBase/generate"

        val jsonRequest = JSONObject().apply {
            put("prompt", prompt)
            put("script", script)
            put("hashtags", hashtags)
            put("instagram_user", settings.instagramUser.ifEmpty { JSONObject.NULL })
            put("instagram_pass", settings.instagramPass.ifEmpty { JSONObject.NULL })
            put("youtube_channel", settings.youtubeChannel.ifEmpty { JSONObject.NULL })
            put("rendering_threads", settings.renderingThreads)
            put("voice", settings.selectedVoice)
        }

        val request = Request.Builder()
            .url(targetUrl)
            .post(jsonRequest.toString().toRequestBody(mediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("FastAPI error: ${response.code} $body"))
            }
            Result.success(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
