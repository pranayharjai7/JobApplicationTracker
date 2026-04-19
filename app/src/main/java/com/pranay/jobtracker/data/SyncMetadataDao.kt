package com.pranay.jobtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE accountId = :accountId LIMIT 1")
    suspend fun getMetadata(accountId: String): SyncMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: SyncMetadata)

    @Query("DELETE FROM sync_metadata")
    suspend fun deleteAll()

    @Query("DELETE FROM sync_metadata WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: String)

    @Query("UPDATE sync_metadata SET accountId = :newAccountId WHERE accountId = 'legacy_account'")
    suspend fun adoptLegacyData(newAccountId: String)
}
