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
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: JobApplicationRepository,
    private val syncManager: com.pranay.jobtracker.domain.GmailSyncManager,
    private val syncEmailsUseCase: com.pranay.jobtracker.domain.SyncEmailsUseCase,
    private val metaRepository: com.pranay.jobtracker.data.SyncMetadataRepository
) : ViewModel() {

    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    val applications: StateFlow<List<JobApplication>> = _applications.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllApplications().collect { apps ->
                _applications.value = apps
            }
        }
    }

    private var syncJob: Job? = null

    fun syncEmails() {
        if (_isSyncing.value) return
        syncJob = viewModelScope.launch {
            _isSyncing.value = true
            try {
                syncEmailsUseCase()
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
            repository.clearAll()
            metaRepository.clearMetadata()
        }
    }
}
