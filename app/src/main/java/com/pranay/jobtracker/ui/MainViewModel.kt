package com.pranay.jobtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranay.jobtracker.data.JobApplication
import com.pranay.jobtracker.data.JobApplicationRepository
import com.pranay.jobtracker.domain.GmailSyncManager
import com.pranay.jobtracker.domain.SyncEmailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel @Inject constructor(
    private val repository: JobApplicationRepository,
    private val syncManager: com.pranay.jobtracker.domain.GmailSyncManager,
    private val syncEmailsUseCase: com.pranay.jobtracker.domain.SyncEmailsUseCase,
    private val metaRepository: com.pranay.jobtracker.data.SyncMetadataRepository,
    val accountRepository: com.pranay.jobtracker.data.AccountRepository
) : ViewModel() {

    val activeAccountFlow = accountRepository.activeAccountIdFlow

    val applications: StateFlow<List<JobApplication>> = accountRepository.activeAccountIdFlow
        .filterNotNull()
        .flatMapLatest { accountId ->
            repository.getAllApplications(accountId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Init block removed since stateIn handles the startup collection

    private var syncJob: Job? = null

    fun syncEmails() {
        if (_isSyncing.value) return
        syncJob = viewModelScope.launch {
            val accountId = accountRepository.getActiveAccountId() ?: return@launch
            _isSyncing.value = true
            try {
                syncEmailsUseCase(accountId)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun stopSyncing() {
        syncJob?.cancel()
        _isSyncing.value = false
    }

    fun clearDatabase() {
        stopSyncing() // Ensure active background jobs are killed immediately
        
        viewModelScope.launch {
            val accountId = accountRepository.getActiveAccountId() ?: return@launch
            repository.clearAccountData(accountId)
            metaRepository.clearMetadata(accountId)
        }
    }
}
