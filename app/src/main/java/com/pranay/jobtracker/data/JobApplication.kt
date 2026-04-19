package com.pranay.jobtracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "applications",
    indices = [
        Index("companyName"),
        Index("jobTitle")
    ]
)
data class JobApplication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyName: String,
    val jobTitle: String,
    val dateApplied: String,
    val status: String,
    val lastUpdate: String,
    val recruiterEmail: String?,
    val notes: String?,
    val emailId: String? = null,
    val createdAt: Long = 0L,
    val lastUpdatedAt: Long = 0L,
    val summary: String? = null,
    val accountId: String = "legacy_account"
)
