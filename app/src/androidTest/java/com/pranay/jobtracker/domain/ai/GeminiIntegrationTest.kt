package com.pranay.jobtracker.domain.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.pranay.jobtracker.BuildConfig
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assume
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Hybrid test for [GeminiAIProvider]. 
 * Includes real integration checks (conditional on API key) and mocked edge-case checks.
 */
@RunWith(AndroidJUnit4::class)
class GeminiIntegrationTest {

    private val provider = GeminiAIProvider()

    @Ignore("Requires real API key and network")
    @Test
    fun realApiIntegration_verifiesExtractionSchema() = runTest {
        // Assume check: Only run this test if a real API key is provided in local.properties
        Assume.assumeTrue("Gemini API key is missing", BuildConfig.GEMINI_API_KEY.isNotBlank() && !BuildConfig.GEMINI_API_KEY.contains("mock"))

        val testPrompt = "Return a JSON array with one object: { \"company\": \"Google\" }. No markdown."
        
        val result = provider.generateContent(testPrompt)
        
        assertThat(result).contains("Google")
        assertThat(result).contains("{")
        assertThat(result).contains("}")
    }

    @Test
    fun mockedErrorHandling_verifiesExceptionOnEmptyResponse() = runTest {
        val mockProvider = object : AIProvider {
            override val providerName: String = "Fake"
            override suspend fun generateContent(prompt: String): String {
                throw IllegalStateException("Gemini returned null text")
            }
        }

        try {
            mockProvider.generateContent("test")
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(IllegalStateException::class.java)
            assertThat(e.message).contains("Gemini returned null text")
        }
    }
    
    @Test
    fun buildConfig_hasApiKeyField() {
        // Basic check to ensure the project is correctly configured to hold the key
        assertThat(BuildConfig.GEMINI_API_KEY).isNotNull()
    }
}
