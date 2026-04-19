package com.pranay.jobtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey val accountId: String,
    val oldestFetchedEpochMs: Long = 0L,   // 0 = never synced
    val lastHistoryId: String? = null      // Incremental sync token
)
