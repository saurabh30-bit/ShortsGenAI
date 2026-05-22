package com.aistudio.shortsgen.data

import android.content.Context
import android.content.SharedPreferences

data class AppSettings(
    val fastapiUrl: String,
    val geminiApiKey: String,
    val instagramUser: String,
    val instagramPass: String,
    val youtubeChannel: String,
    val renderingThreads: Int,
    val selectedVoice: String,
    val isSimulationMode: Boolean
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shortsgen_prefs", Context.MODE_PRIVATE)

    /**
     * Called once on first launch to pre-populate defaults.
     * We check a "first_launch" flag so this only runs once.
     */
    init {
        if (!prefs.getBoolean("first_launch_done", false)) {
            prefs.edit().apply {
                // Default to Live mode (not simulation)
                putBoolean("is_simulation_mode", false)
                // Mark first launch done
                putBoolean("first_launch_done", true)
                apply()
            }
        }
    }

    fun getSettings(): AppSettings {
        return AppSettings(
            fastapiUrl = prefs.getString("fastapi_url", "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000",
            geminiApiKey = prefs.getString("gemini_api_key", "") ?: "",
            instagramUser = prefs.getString("instagram_user", "") ?: "",
            instagramPass = prefs.getString("instagram_pass", "") ?: "",
            youtubeChannel = prefs.getString("youtube_channel", "") ?: "",
            renderingThreads = prefs.getInt("rendering_threads", 4),
            selectedVoice = prefs.getString("selected_voice", "en_us_male") ?: "en_us_male",
            isSimulationMode = prefs.getBoolean("is_simulation_mode", false)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit().apply {
            putString("fastapi_url", settings.fastapiUrl)
            putString("gemini_api_key", settings.geminiApiKey)
            putString("instagram_user", settings.instagramUser)
            putString("instagram_pass", settings.instagramPass)
            putString("youtube_channel", settings.youtubeChannel)
            putInt("rendering_threads", settings.renderingThreads)
            putString("selected_voice", settings.selectedVoice)
            putBoolean("is_simulation_mode", settings.isSimulationMode)
            apply()
        }
    }
}
