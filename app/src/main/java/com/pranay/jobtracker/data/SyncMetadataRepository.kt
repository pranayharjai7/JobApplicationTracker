package com.pranay.jobtracker.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncMetadataRepository @Inject constructor(private val dao: SyncMetadataDao) {

    suspend fun getMetadata(accountId: String): SyncMetadata = dao.getMetadata(accountId) ?: SyncMetadata(accountId)

    suspend fun saveMetadata(metadata: SyncMetadata) = dao.upsertMetadata(metadata)

    suspend fun clearMetadata(accountId: String) = dao.deleteByAccount(accountId)
}
