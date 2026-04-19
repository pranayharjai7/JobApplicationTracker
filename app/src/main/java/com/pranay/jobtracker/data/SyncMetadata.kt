package com.pranay.jobtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata")
data class SyncMetadata(
    @PrimaryKey val id: Int = 1,
    val oldestFetchedEpochMs: Long = 0L   // 0 = never synced
)
