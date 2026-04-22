package com.pranay.jobtracker.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobApplicationRepository @Inject constructor(
    private val dao: ApplicationDao
) {
    fun getAllApplications(accountId: String): Flow<List<JobApplication>> = dao.getAllApplications(accountId)

    fun getDistinctCompanies(accountId: String): Flow<List<String>> = dao.getDistinctCompanies(accountId)

    fun getApplicationsByCompanies(accountId: String, companies: List<String>): Flow<List<JobApplication>> = dao.getApplicationsByCompanies(accountId, companies)

    fun getApplicationById(id: Int, accountId: String): Flow<JobApplication?> = dao.getApplicationById(id, accountId)

    suspend fun getApplicationByIdStandalone(id: Int): JobApplication? = dao.getApplicationByIdStandalone(id)

    suspend fun getExistingEmailIds(emailIds: List<String>, accountId: String): List<String> = dao.getExistingEmailIds(emailIds, accountId)

    suspend fun insertApplications(applications: List<JobApplication>) {
        dao.insertApplications(applications)
    }

    suspend fun insertSingleApplication(application: JobApplication): Long =
        dao.insertSingleApplication(application)

    suspend fun findByNormalizedKey(normalizedCompany: String, normalizedTitle: String, accountId: String): JobApplication? =
        dao.findByNormalizedKey(normalizedCompany, normalizedTitle, accountId)

    suspend fun updateApplicationStatus(id: Int, status: String, lastUpdatedAt: Long, lastUpdate: String, summary: String?) =
        dao.updateStatusAndTimestamp(id, status, lastUpdate, lastUpdatedAt, summary)

    suspend fun updateApplication(application: JobApplication) =
        dao.updateApplication(application)

    suspend fun clearAccountData(accountId: String) {
        dao.deleteByAccount(accountId)
    }
}
