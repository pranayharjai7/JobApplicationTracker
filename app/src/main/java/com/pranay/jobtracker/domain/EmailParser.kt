package com.pranay.jobtracker.domain

import com.pranay.jobtracker.data.JobApplication

/**
 * Interface representing the AI/NLP Service that parses raw email text
 * into structured [ParsedEmailInfo] for each detected job application.
 */
data class RawEmailData(
    val emailId: String,      // Gmail message ID — no longer smuggled inside date
    val subject: String,
    val body: String,
    val date: String,         // clean RFC 2822 date string only
    val from: String = ""     // From header value
)

interface EmailParser {
    suspend fun parseEmailBatch(emails: List<RawEmailData>): List<ParsedEmailInfo>
}

class MockEmailParserImpl : EmailParser {
    override suspend fun parseEmailBatch(emails: List<RawEmailData>): List<ParsedEmailInfo> {
        return emails.mapNotNull { email ->
            if (email.subject.contains("Google", ignoreCase = true)) {
                ParsedEmailInfo(
                    sourceEmailId = email.emailId,
                    companyName   = "Google",
                    jobTitle      = "Software Engineer",
                    status        = "Interview Scheduled",
                    stage         = "INTERVIEW",
                    subStatus     = null,
                    dateStr       = email.date,
                    dateEpochMs   = 0L,
                    recruiterEmail = "recruiting@google.com",
                    snippet       = email.body
                )
            } else null
        }
    }
}
