package com.pranay.jobtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountInfoDao {

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveAccounts(): Flow<List<AccountInfo>>

    @Query("SELECT * FROM accounts WHERE accountId = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: AccountInfo)

    @Query("UPDATE accounts SET isActive = 0 WHERE accountId = :id")
    suspend fun deactivateAccount(id: String)

    @Query("DELETE FROM accounts WHERE accountId = :id")
    suspend fun deleteAccountCompletely(id: String)
}
