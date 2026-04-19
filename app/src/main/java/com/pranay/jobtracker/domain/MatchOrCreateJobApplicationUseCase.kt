package com.pranay.jobtracker.domain

import com.pranay.jobtracker.data.JobApplication
import com.pranay.jobtracker.data.JobApplicationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MatchOrCreateJobApplicationUseCase @Inject constructor(
    private val appRepository: JobApplicationRepository
) {
    /**
     * Returns (jobApplication, isNew).
     * Matches on normalized companyName + jobTitle via DB query.
     * Creates a new entry when either field is blank or no match is found.
     */
    suspend operator fun invoke(parsedInfo: ParsedEmailInfo): Pair<JobApplication, Boolean> {
        val normCompany = EmailMatcher.normalize(parsedInfo.companyName)
        val normTitle   = EmailMatcher.normalize(parsedInfo.jobTitle)

        val existing = if (normCompany.isNotBlank() && normTitle.isNotBlank()) {
            appRepository.findByNormalizedKey(normCompany, normTitle)
        } else null

        return if (existing != null) {
            Pair(existing, false)
        } else {
            val newApp = JobApplication(
                companyName    = parsedInfo.companyName.ifBlank { "Unknown Company" },
                jobTitle       = parsedInfo.jobTitle.ifBlank { "Unknown Role" },
                dateApplied    = parsedInfo.dateStr.ifBlank { "Recent" },
                status         = parsedInfo.status.ifBlank { "Unknown" },
                lastUpdate     = parsedInfo.dateStr.ifBlank { "Recent" },
                recruiterEmail = parsedInfo.recruiterEmail,
                notes          = null,
                emailId        = parsedInfo.sourceEmailId,
                createdAt      = parsedInfo.dateEpochMs,
                lastUpdatedAt  = parsedInfo.dateEpochMs
            )
            val id = appRepository.insertSingleApplication(newApp)
            Pair(newApp.copy(id = id.toInt()), true)
        }
    }
}
