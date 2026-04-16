package com.pranay.jobtracker.domain

import com.pranay.jobtracker.data.JobApplication

/**
 * Interface representing the AI/NLP Service that parses raw email text
 * into a structured [JobApplication].
 */
data class RawEmailData(val subject: String, val body: String, val date: String)

interface EmailParser {
    suspend fun parseEmailBatch(emails: List<RawEmailData>): List<JobApplication>
}

class MockEmailParserImpl : EmailParser {
    override suspend fun parseEmailBatch(emails: List<RawEmailData>): List<JobApplication> {
        return emails.mapNotNull { email ->
            if (email.subject.contains("Google", ignoreCase = true)) {
                JobApplication(
                    companyName = "Google",
                    jobTitle = "Software Engineer",
                    dateApplied = email.date,
                    status = "Interview Scheduled",
                    lastUpdate = email.date,
                    recruiterEmail = "recruiting@google.com",
                    notes = "Mock parsed from batched subject: ${email.subject}"
                )
            } else null
        }
    }
}
