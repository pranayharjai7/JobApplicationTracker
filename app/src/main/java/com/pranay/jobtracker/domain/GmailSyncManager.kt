package com.pranay.jobtracker.domain

import com.pranay.jobtracker.data.JobApplicationRepository
import kotlinx.coroutines.delay

interface GmailSyncManager {
    suspend fun syncRecentJobEmails()
}

class MockGmailSyncManagerImpl(
    private val repository: JobApplicationRepository,
    private val emailParser: EmailParser
) : GmailSyncManager {
    override suspend fun syncRecentJobEmails() {
        // Simulate network delay
        delay(1500)
        
        // Mock emails fetched from Gmail API
        val mockEmails = listOf(
            Pair("Your application to Google for Software Engineer", "2024-03-12"),
            Pair("Thank you for applying to Meta", "2024-03-15"),
            Pair("Random newsletter", "2024-03-16")
        )

        val rawEmails = mockEmails.map { (subject, date) ->
            RawEmailData(subject, "Some mock email body", date)
        }

        val parsedApplications = emailParser.parseEmailBatch(rawEmails)
        
        if (parsedApplications.isNotEmpty()) {
            repository.insertApplications(parsedApplications)
        }
    }
}
