package com.pranay.jobtracker.domain.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.pranay.jobtracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiAIProvider : AIProvider {
    override val providerName: String = "Gemini"

    private val generativeModel = GenerativeModel(
        modelName = "gemini-flash-latest",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    override suspend fun generateContent(prompt: String): String {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            throw IllegalStateException("Gemini API key is blank")
        }

        return withContext(Dispatchers.IO) {
            Log.d("AIProvider", "Provider: $providerName | Model: gemini-flash-latest | Action: Processing prompt")
            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                }
            )
            response.text ?: throw IllegalStateException("Gemini returned null text")
        }
    }
}
