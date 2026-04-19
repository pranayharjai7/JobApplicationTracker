package com.pranay.jobtracker.domain

import com.pranay.jobtracker.domain.ai.AIProviderFactory
import com.pranay.jobtracker.data.EmailEventRepository
import com.pranay.jobtracker.data.JobApplicationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GenerateJobSummaryUseCase @Inject constructor(
    private val eventRepository: EmailEventRepository,
    private val appRepository: JobApplicationRepository,
    private val aiProviderFactory: AIProviderFactory
) {

    suspend operator fun invoke(
        jobApplicationId: Int,
        companyName: String,
        jobTitle: String,
        currentStatus: String,
        lastUpdatedAt: Long,
        lastUpdate: String
    ) {
        val events = eventRepository.getRecentEventsForSummary(jobApplicationId, limit = 10)
        if (events.isEmpty()) return

        // Events come back DESC; reverse to get chronological order for the prompt
        val timeline = events.reversed().joinToString("\n") { event ->
            "- [${event.date}] Status: ${event.detectedStatus} | ${event.snippet.take(200)}"
        }

        val prompt = """
            You are summarizing a job application journey for a user's job tracker app.
            Company: $companyName
            Role: $jobTitle
            Current Status: $currentStatus

            Email timeline (oldest to newest):
            $timeline

            Write a single concise paragraph (2-4 sentences) summarizing what has happened in this
            application process so far. Focus on key milestones and the current state.
            Do not use markdown. Do not use bullet points. Write in third person.
            Respond ONLY with the summary paragraph — no other text.
        """.trimIndent()

        val summary = runCatching {
            val provider = aiProviderFactory.getProvider()
            provider.generateContent(prompt).trim()
        }.getOrNull() ?: return

        appRepository.updateApplicationStatus(
            id            = jobApplicationId,
            status        = currentStatus,
            lastUpdatedAt = lastUpdatedAt,
            lastUpdate    = lastUpdate,
            summary       = summary
        )
    }
}
