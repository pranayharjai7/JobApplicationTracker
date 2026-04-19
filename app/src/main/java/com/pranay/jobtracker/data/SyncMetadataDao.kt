package com.pranay.jobtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE id = 1 LIMIT 1")
    suspend fun getMetadata(): SyncMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: SyncMetadata)

    @Query("DELETE FROM sync_metadata")
    suspend fun deleteAll()
}
