package com.pranay.jobtracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "email_events",
    foreignKeys = [ForeignKey(
        entity = JobApplication::class,
        parentColumns = ["id"],
        childColumns = ["jobApplicationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("jobApplicationId"),
        Index(value = ["gmailMessageId"], unique = true)
    ]
)
data class EmailEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jobApplicationId: Int,
    val gmailMessageId: String,
    val date: String,
    val dateEpochMs: Long,
    val snippet: String,
    val detectedStatus: String,
    val summary: String? = null,
    val accountId: String = "legacy_account"
)
