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

    suspend fun insertSingleApplication(application: JobApplication): Long =
        dao.insertSingleApplication(application)

    suspend fun findByNormalizedKey(normalizedCompany: String, normalizedTitle: String): JobApplication? =
        dao.findByNormalizedKey(normalizedCompany, normalizedTitle)

    suspend fun updateApplicationStatus(id: Int, status: String, lastUpdatedAt: Long, lastUpdate: String, summary: String?) =
        dao.updateStatusAndTimestamp(id, status, lastUpdate, lastUpdatedAt, summary)

    suspend fun clearAll() {
        dao.deleteAll()
    }
}
