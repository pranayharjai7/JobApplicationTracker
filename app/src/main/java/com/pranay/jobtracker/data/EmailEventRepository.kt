package com.pranay.jobtracker.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailEventRepository @Inject constructor(private val dao: EmailEventDao) {

    fun getEventsForApplication(jobApplicationId: Int): Flow<List<EmailEvent>> =
        dao.getEventsForApplication(jobApplicationId)

    fun getAllEventsForAccount(accountId: String): Flow<List<EmailEvent>> =
        dao.getAllEventsForAccount(accountId)

    suspend fun getEventByGmailId(gmailMessageId: String): EmailEvent? =
        dao.getEventByGmailId(gmailMessageId)

    suspend fun insertEvent(event: EmailEvent): Long = dao.insertEvent(event)

    suspend fun getRecentEventsForSummary(jobApplicationId: Int, limit: Int = 10): List<EmailEvent> =
        dao.getRecentEventsForSummary(jobApplicationId, limit)
}
