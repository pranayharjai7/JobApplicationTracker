package com.pranay.jobtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailEventDao {

    @Query("SELECT * FROM email_events WHERE jobApplicationId = :jobApplicationId ORDER BY dateEpochMs ASC")
    fun getEventsForApplication(jobApplicationId: Int): Flow<List<EmailEvent>>

    @Query("SELECT * FROM email_events WHERE gmailMessageId = :gmailMessageId LIMIT 1")
    suspend fun getEventByGmailId(gmailMessageId: String): EmailEvent?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: EmailEvent): Long

    @Query("SELECT * FROM email_events WHERE jobApplicationId = :jobApplicationId ORDER BY dateEpochMs DESC LIMIT :limit")
    suspend fun getRecentEventsForSummary(jobApplicationId: Int, limit: Int): List<EmailEvent>

    @Query("DELETE FROM email_events")
    suspend fun deleteAll()

    @Query("DELETE FROM email_events WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)

    @Query("UPDATE email_events SET accountId = :newAccountId WHERE accountId = 'legacy_account'")
    suspend fun adoptLegacyData(newAccountId: String)
}
