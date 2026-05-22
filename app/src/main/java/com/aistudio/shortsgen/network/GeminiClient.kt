package com.aistudio.shortsgen.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // Use gemini-2.0-flash as the primary model, fallback to 1.5-flash
    private val modelEndpoints = listOf(
        "gemini-2.0-flash",
        "gemini-1.5-flash"
    )

    suspend fun generateScriptAndHashtags(
        apiKey: String,
        topicPrompt: String
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val prompt = """
            Write a short-form video script about: $topicPrompt.
            Keep the script engaging, punchy, and suitable for a 30-60 second vertical video.
            Do NOT use any markdown formatting (no asterisks, no bold, no headers, no bullet dashes).
            Write in a conversational, energetic tone suitable for Instagram Reels / YouTube Shorts.
            After the script, on a new line, generate 5 to 10 relevant hashtags.
            
            Your response MUST follow this EXACT format (include the separator lines):
            ---SCRIPT---
            [Your script here]
            ---HASHTAGS---
            [Your hashtags here, e.g., #shorts #viral #trending]
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.9)
                put("maxOutputTokens", 1024)
            })
        }

        var lastError: Exception? = null

        for (model in modelEndpoints) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonRequest.toString().toRequestBody(mediaType))
                .addHeader("Content-Type", "application/json")
                .build()

            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    // Parse error from body if possible
                    val errMsg = try {
                        JSONObject(body).optJSONObject("error")?.optString("message") ?: body
                    } catch (_: Exception) { body }
                    lastError = Exception("[$model] API error ${response.code}: $errMsg")
                    // If it's a model-not-found error, try next model; otherwise fail fast
                    if (response.code == 404) continue else break
                }

                if (body.isBlank()) {
                    lastError = Exception("[$model] Empty response body")
                    continue
                }

                val json = JSONObject(body)
                if (json.has("error")) {
                    val errMsg = json.getJSONObject("error").optString("message", "Unknown error")
                    lastError = Exception("[$model] $errMsg")
                    continue
                }

                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    // Check for promptFeedback block
                    val feedback = json.optJSONObject("promptFeedback")
                    val reason = feedback?.optString("blockReason", "No candidates returned")
                    lastError = Exception("[$model] Blocked or no candidates: $reason")
                    continue
                }

                val content = candidates.getJSONObject(0)
                    .optJSONObject("content")
                    ?: run { lastError = Exception("[$model] No content in candidate"); continue }

                val parts = content.optJSONArray("parts")
                if (parts == null || parts.length() == 0) {
                    lastError = Exception("[$model] No parts in content")
                    continue
                }

                val fullText = parts.getJSONObject(0).optString("text", "")
                if (fullText.isBlank()) {
                    lastError = Exception("[$model] Empty text in response")
                    continue
                }

                return@withContext parseScriptAndHashtags(fullText)
            } catch (e: Exception) {
                lastError = e
                // Network errors - don't retry with another model
                break
            }
        }

        Result.failure(lastError ?: Exception("All Gemini models failed"))
    }

    private fun parseScriptAndHashtags(fullText: String): Result<Pair<String, String>> {
        val scriptMarker = "---SCRIPT---"
        val hashtagsMarker = "---HASHTAGS---"

        return if (fullText.contains(scriptMarker) && fullText.contains(hashtagsMarker)) {
            val scriptStart = fullText.indexOf(scriptMarker) + scriptMarker.length
            val hashtagsStart = fullText.indexOf(hashtagsMarker)
            var script = fullText.substring(scriptStart, hashtagsStart).trim()
            var hashtags = fullText.substring(hashtagsStart + hashtagsMarker.length).trim()

            // Strip markdown artifacts
            script = script.replace(Regex("[*_`]"), "").trim()
            hashtags = hashtags.replace(Regex("[*_`]"), "").trim()

            Result.success(Pair(script, hashtags))
        } else {
            // Fallback: last line with # is hashtags, rest is script
            val lines = fullText.lines().filter { it.isNotBlank() }
            val hashtagLine = lines.lastOrNull { l -> l.contains("#") } ?: ""
            val script = lines
                .filter { it != hashtagLine }
                .joinToString("\n")
                .replace(Regex("[*_`#]{1,2}[^#\\s].*"), "")
                .replace(Regex("[*_`]"), "")
                .trim()
            val hashtags = hashtagLine.trim()
            Result.success(Pair(script, hashtags))
        }
    }
}
