package com.pranay.jobtracker.domain.ai

import android.util.Log

class FallbackAIProvider(
    private val providers: List<AIProvider>
) : AIProvider {
    override val providerName: String = "FallbackChain[${providers.joinToString { it.providerName }}]"

    override suspend fun generateContent(prompt: String): String {
        if (providers.isEmpty()) {
            throw IllegalStateException("No AI providers configured.")
        }

        var lastException: Exception? = null

        for (provider in providers) {
            try {
                return provider.generateContent(prompt)
            } catch (e: Exception) {
                Log.e("AIProvider", "Provider: ${provider.providerName} failed.", e)
                lastException = e
                // Continue to the next provider in the chain
            }
        }

        // If we reach here, all providers failed
        throw IllegalStateException("All configured AI providers failed. Last exception: ${lastException?.message}", lastException)
    }
}
