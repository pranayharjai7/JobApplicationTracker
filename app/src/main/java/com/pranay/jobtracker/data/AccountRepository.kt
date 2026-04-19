package com.pranay.jobtracker.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "account_prefs")

@Singleton
class AccountRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val accountDao: AccountInfoDao,
    private val appDao: ApplicationDao,
    private val emailEventDao: EmailEventDao,
    private val syncMetaDao: SyncMetadataDao
) {
    private val ACTIVE_ACCOUNT_KEY = stringPreferencesKey("active_account_id")

    val activeAccountIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACTIVE_ACCOUNT_KEY]
    }

    suspend fun getActiveAccountId(): String? {
        return context.dataStore.data.first()[ACTIVE_ACCOUNT_KEY]
    }

    suspend fun setActiveAccount(accountId: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_ACCOUNT_KEY] = accountId
        }
    }

    fun getActiveAccountsFlow(): Flow<List<AccountInfo>> {
        return accountDao.getActiveAccounts()
    }

    suspend fun addOrUpdateAccount(email: String, displayName: String, photoUrl: String): AccountInfo {
        // Generating deterministic hash color 
        val colors = listOf(
            "#4285F4", // Blue
            "#0F9D58", // Green
            "#DB4437", // Red
            "#F4B400", // Yellow
            "#9C27B0", // Purple
            "#009688", // Teal
            "#FF9800", // Orange
            "#FFEB3B"  // Amber
        )
        val hash = kotlin.math.abs(email.hashCode())
        val colorHash = colors[hash % colors.size]

        val existing = accountDao.getAccountById(email)
        val account = existing?.copy(
            displayName = displayName,
            photoUrl = photoUrl,
            isActive = true
        ) ?: AccountInfo(
            accountId = email,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            colorHash = colorHash,
            isActive = true
        )

        // If it's the first account ever, we should adopt legacy data
        val activeAccounts = accountDao.getActiveAccounts().first()
        if (activeAccounts.isEmpty()) {
            appDao.adoptLegacyData(email)
            emailEventDao.adoptLegacyData(email)
            // For sync meta, since MIGRATION_4_5 already ported, let's just update the ID and it's fine.
            syncMetaDao.adoptLegacyData(email)
        }

        accountDao.upsertAccount(account)
        return account
    }

    suspend fun signOutAccount(accountId: String) {
        accountDao.deactivateAccount(accountId)
        val nextActive = accountDao.getActiveAccounts().first().firstOrNull()?.accountId
        if (nextActive != null) {
            setActiveAccount(nextActive)
        } else {
            // Nullify if no other accounts exist
            context.dataStore.edit { prefs -> prefs.remove(ACTIVE_ACCOUNT_KEY) }
        }
    }

    suspend fun removeAccountAndData(accountId: String) {
        // Execute manual transactional wiping since we don't have SQL Cascade ON DELETE for everything
        appDao.deleteByAccount(accountId)
        emailEventDao.deleteByAccount(accountId)
        syncMetaDao.deleteByAccount(accountId)
        accountDao.deleteAccountCompletely(accountId)

        val activeId = getActiveAccountId()
        if (activeId == accountId) {
            val nextActive = accountDao.getActiveAccounts().first().firstOrNull()?.accountId
            if (nextActive != null) {
                setActiveAccount(nextActive)
            } else {
                context.dataStore.edit { prefs -> prefs.remove(ACTIVE_ACCOUNT_KEY) }
            }
        }
    }
}
