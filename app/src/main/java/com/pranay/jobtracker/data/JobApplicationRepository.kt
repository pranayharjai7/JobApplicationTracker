package com.pranay.jobtracker.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobApplicationRepository @Inject constructor(
    private val dao: ApplicationDao
) {
    fun getAllApplications(): Flow<List<JobApplication>> = dao.getAllApplications()

    fun getApplicationById(id: Int): Flow<JobApplication?> = dao.getApplicationById(id)

    suspend fun getExistingEmailIds(emailIds: List<String>): List<String> = dao.getExistingEmailIds(emailIds)

    suspend fun insertApplications(applications: List<JobApplication>) {
        dao.insertApplications(applications)
    }
    
    suspend fun clearAll() {
        dao.deleteAll()
    }
}
