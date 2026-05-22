package com.aistudio.shortsgen.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class WikipediaClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateScriptFromWikipedia(topic: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val encodedTopic = URLEncoder.encode(topic, "UTF-8")
            val url = "https://en.wikipedia.org/w/api.php?action=query&prop=extracts&exsentences=6&exlimit=1&titles=$encodedTopic&explaintext=1&format=json"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful || body.isBlank()) {
                return@withContext Result.failure(Exception("Wikipedia API failed: ${response.code}"))
            }

            val json = JSONObject(body)
            val pages = json.getJSONObject("query").getJSONObject("pages")
            val pageId = pages.keys().next()
            
            if (pageId == "-1") {
                return@withContext Result.failure(Exception("Could not find any Wikipedia article for: $topic"))
            }

            val extract = pages.getJSONObject(pageId).optString("extract", "")
            if (extract.isBlank()) {
                return@withContext Result.failure(Exception("No summary available for: $topic"))
            }

            // Clean up the extract to sound more like a script
            val script = "Did you know? " + extract
                .replace(Regex("\\[\\d+\\]"), "") // Remove citations like [1]
                .replace(Regex("\\(.*?\\)"), "") // Remove parenthetical pronunciations/dates
                .replace("  ", " ")
                .trim() + "\n\nLike and subscribe for more facts!"

            // Generate some simple hashtags based on the words
            val words = topic.split(Regex("\\s+")).filter { it.length > 3 }.take(3)
            val hashtags = "#shorts #facts " + words.joinToString(" ") { "#${it.lowercase()}" }

            Result.success(Pair(script, hashtags))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
