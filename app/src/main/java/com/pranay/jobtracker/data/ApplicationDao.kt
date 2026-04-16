package com.pranay.jobtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("DELETE FROM applications")
    suspend fun deleteAll()
}
