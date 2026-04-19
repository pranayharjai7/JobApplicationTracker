package com.pranay.jobtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {
    @Query("SELECT * FROM applications ORDER BY lastUpdate DESC")
    fun getAllApplications(): Flow<List<JobApplication>>

    @Query("SELECT * FROM applications WHERE id = :id LIMIT 1")
    fun getApplicationById(id: Int): Flow<JobApplication?>

    @Query("SELECT emailId FROM applications WHERE emailId IN (:emailIds) AND emailId IS NOT NULL")
    suspend fun getExistingEmailIds(emailIds: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(applications: List<JobApplication>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleApplication(application: JobApplication): Long

    @Update
    suspend fun updateApplication(application: JobApplication)

    @Query("SELECT * FROM applications WHERE lower(trim(companyName)) = :normalizedCompany AND lower(trim(jobTitle)) = :normalizedTitle LIMIT 1")
    suspend fun findByNormalizedKey(normalizedCompany: String, normalizedTitle: String): JobApplication?

    @Query("UPDATE applications SET status = :status, lastUpdate = :lastUpdate, lastUpdatedAt = :lastUpdatedAt, summary = :summary WHERE id = :id")
    suspend fun updateStatusAndTimestamp(id: Int, status: String, lastUpdate: String, lastUpdatedAt: Long, summary: String?)

    @Query("DELETE FROM applications")
    suspend fun deleteAll()
}
