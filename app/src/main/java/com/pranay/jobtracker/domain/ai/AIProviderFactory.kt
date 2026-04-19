package com.pranay.jobtracker.domain.ai

import com.pranay.jobtracker.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIProviderFactory @Inject constructor() {
    
    fun getProvider(): AIProvider {
        val configuredProviders = mutableListOf<AIProvider>()

        // Order specified by user requirement: Groq -> Gemini -> OpenRouter
        if (BuildConfig.GROQ_API_KEY.isNotBlank()) {
            configuredProviders.add(GroqAIProvider())
        }
        
        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            configuredProviders.add(GeminiAIProvider())
        }

        if (BuildConfig.OPENROUTER_API_KEY.isNotBlank()) {
            configuredProviders.add(OpenRouterAIProvider())
        }

        return FallbackAIProvider(configuredProviders)
    }
}
