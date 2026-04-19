package com.pranay.jobtracker.domain

import com.pranay.jobtracker.data.EmailEvent
import com.pranay.jobtracker.data.EmailEventRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddEmailEventUseCase @Inject constructor(
    private val eventRepository: EmailEventRepository
) {
    /**
     * Inserts a new EmailEvent for the given jobApplicationId.
     * Returns true if inserted, false if the gmailMessageId was already present.
     */
    suspend operator fun invoke(parsedInfo: ParsedEmailInfo, jobApplicationId: Int): Boolean {
        if (eventRepository.getEventByGmailId(parsedInfo.sourceEmailId) != null) return false

        val event = EmailEvent(
            jobApplicationId = jobApplicationId,
            gmailMessageId   = parsedInfo.sourceEmailId,
            date             = parsedInfo.dateStr,
            dateEpochMs      = parsedInfo.dateEpochMs,
            snippet          = parsedInfo.snippet,
            detectedStatus   = parsedInfo.status
        )
        eventRepository.insertEvent(event)
        return true
    }
}
