package com.pranay.jobtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {
    @Query("SELECT * FROM applications WHERE accountId = :accountId ORDER BY lastUpdatedAt DESC")
    fun getAllApplications(accountId: String): Flow<List<JobApplication>>

    @Query("SELECT DISTINCT companyName FROM applications WHERE accountId = :accountId AND companyName IS NOT NULL AND companyName != '' ORDER BY companyName ASC")
    fun getDistinctCompanies(accountId: String): Flow<List<String>>

    @Query("SELECT * FROM applications WHERE accountId = :accountId AND companyName IN (:companies) ORDER BY lastUpdatedAt DESC")
    fun getApplicationsByCompanies(accountId: String, companies: List<String>): Flow<List<JobApplication>>

    @Query("SELECT * FROM applications WHERE id = :id AND accountId = :accountId LIMIT 1")
    fun getApplicationById(id: Int, accountId: String): Flow<JobApplication?>

    @Query("SELECT emailId FROM applications WHERE emailId IN (:emailIds) AND emailId IS NOT NULL AND accountId = :accountId")
    suspend fun getExistingEmailIds(emailIds: List<String>, accountId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplications(applications: List<JobApplication>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleApplication(application: JobApplication): Long

    @Update
    suspend fun updateApplication(application: JobApplication)

    @Query("SELECT * FROM applications WHERE lower(trim(companyName)) = :normalizedCompany AND lower(trim(jobTitle)) = :normalizedTitle AND accountId = :accountId LIMIT 1")
    suspend fun findByNormalizedKey(normalizedCompany: String, normalizedTitle: String, accountId: String): JobApplication?

    @Query("UPDATE applications SET status = :status, lastUpdate = :lastUpdate, lastUpdatedAt = :lastUpdatedAt, summary = :summary WHERE id = :id")
    suspend fun updateStatusAndTimestamp(id: Int, status: String, lastUpdate: String, lastUpdatedAt: Long, summary: String?)

    @Query("DELETE FROM applications")
    suspend fun deleteAll()

    @Query("DELETE FROM applications WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)

    @Query("UPDATE applications SET accountId = :newAccountId WHERE accountId = 'legacy_account'")
    suspend fun adoptLegacyData(newAccountId: String)
}
