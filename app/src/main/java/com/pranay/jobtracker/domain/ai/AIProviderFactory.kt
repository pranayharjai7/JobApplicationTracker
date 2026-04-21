package com.pranay.jobtracker.domain.ai

import com.pranay.jobtracker.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIProviderFactory @Inject constructor() {
    
    private var testProviders: List<AIProvider>? = null

    // For testing
    constructor(providers: List<AIProvider>) : this() {
        this.testProviders = providers
    }

    fun getProvider(): AIProvider {
        if (testProviders != null) return FallbackAIProvider(testProviders!!)

        val configuredProviders = mutableListOf<AIProvider>()
        // ... rest of the logic

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
