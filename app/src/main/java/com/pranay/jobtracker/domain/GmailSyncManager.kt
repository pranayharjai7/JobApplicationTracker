package com.pranay.jobtracker.domain

import com.pranay.jobtracker.data.JobApplication
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
        delay(1500)

        val mockEmails = listOf(
            Pair("Your application to Google for Software Engineer", "2024-03-12"),
            Pair("Thank you for applying to Meta", "2024-03-15"),
            Pair("Random newsletter", "2024-03-16")
        )

        val rawEmails = mockEmails.map { (subject, date) ->
            RawEmailData(
                emailId = "mock-${subject.hashCode()}",
                subject = subject,
                body = "Some mock email body",
                date = date
            )
        }

        val parsed: List<ParsedEmailInfo> = emailParser.parseEmailBatch(rawEmails)

        if (parsed.isNotEmpty()) {
            val applications = parsed.map { info ->
                JobApplication(
                    companyName    = info.companyName,
                    jobTitle       = info.jobTitle,
                    dateApplied    = info.dateStr,
                    status         = info.status,
                    lastUpdate     = info.dateStr,
                    recruiterEmail = info.recruiterEmail,
                    notes          = null,
                    emailId        = info.sourceEmailId
                )
            }
            repository.insertApplications(applications)
        }
    }
}
