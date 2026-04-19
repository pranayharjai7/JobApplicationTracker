package com.pranay.jobtracker.domain.ai

import android.util.Log
import com.google.gson.Gson
import com.pranay.jobtracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class OpenRouterAIProvider : AIProvider {
    override val providerName: String = "OpenRouter"
    private val endpoint = "https://openrouter.ai/api/v1/chat/completions"
    private val modelName = "meta-llama/llama-3.1-8b-instruct"
    private val gson = Gson()

    override suspend fun generateContent(prompt: String): String {
        if (BuildConfig.OPENROUTER_API_KEY.isBlank()) {
            throw IllegalStateException("OpenRouter API key is blank")
        }

        return withContext(Dispatchers.IO) {
            Log.d("AIProvider", "Provider: $providerName | Model: $modelName | Action: Processing prompt")

            val url = URL(endpoint)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer ${BuildConfig.OPENROUTER_API_KEY}")
                setRequestProperty("HTTP-Referer", "com.pranay.jobtracker") // OpenRouter requests recommended this
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 30000
                readTimeout = 60000
            }

            val payload = mapOf(
                "model" to modelName,
                "messages" to listOf(
                    mapOf("role" to "user", "content" to prompt)
                )
            )

            val jsonPayload = gson.toJson(payload)
            connection.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseString = InputStreamReader(connection.inputStream).readText()
                val responseMap = gson.fromJson(responseString, Map::class.java)
                
                val choices = responseMap["choices"] as? List<Map<String, Any>>
                val message = choices?.firstOrNull()?.get("message") as? Map<String, Any>
                val content = message?.get("content") as? String
                content ?: throw IllegalStateException("OpenRouter returned empty content")
            } else {
                val errorStream = connection.errorStream?.let { InputStreamReader(it).readText() } ?: "No error body"
                throw IllegalStateException("OpenRouter APIs failed with code: $responseCode Body: $errorStream")
            }
        }
    }
}
