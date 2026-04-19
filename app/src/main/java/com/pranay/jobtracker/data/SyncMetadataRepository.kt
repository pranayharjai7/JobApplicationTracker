package com.pranay.jobtracker.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncMetadataRepository @Inject constructor(private val dao: SyncMetadataDao) {

    suspend fun getMetadata(): SyncMetadata = dao.getMetadata() ?: SyncMetadata()

    suspend fun saveMetadata(metadata: SyncMetadata) = dao.upsertMetadata(metadata)

    suspend fun clearMetadata() = dao.deleteAll()
}
