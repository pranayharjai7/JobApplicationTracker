package com.pranay.jobtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountInfo(
    @PrimaryKey val accountId: String,
    val email: String,
    val displayName: String,
    val photoUrl: String,
    val colorHash: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
