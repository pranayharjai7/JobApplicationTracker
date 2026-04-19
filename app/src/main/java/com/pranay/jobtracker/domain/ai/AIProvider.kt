package com.pranay.jobtracker.domain.ai

interface AIProvider {
    val providerName: String
    suspend fun generateContent(prompt: String): String
}
