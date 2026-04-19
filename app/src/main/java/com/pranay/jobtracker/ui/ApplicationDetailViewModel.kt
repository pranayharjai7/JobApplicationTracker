package com.pranay.jobtracker.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranay.jobtracker.data.EmailEvent
import com.pranay.jobtracker.data.EmailEventRepository
import com.pranay.jobtracker.data.JobApplication
import com.pranay.jobtracker.data.JobApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ApplicationDetailViewModel @Inject constructor(
    repository: JobApplicationRepository,
    eventRepository: EmailEventRepository,
    accountRepository: com.pranay.jobtracker.data.AccountRepository,
    private val accountDao: com.pranay.jobtracker.data.AccountInfoDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val id: Int = checkNotNull(savedStateHandle["id"])

    val application: StateFlow<JobApplication?> = accountRepository.activeAccountIdFlow
        .filterNotNull()
        .flatMapLatest { accountId ->
            repository.getApplicationById(id, accountId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val events: StateFlow<List<EmailEvent>> = accountRepository.activeAccountIdFlow
        .filterNotNull()
        .flatMapLatest { accountId ->
            // eventRepository already scopes events by jobApplicationId, but let's be technically pure
            eventRepository.getEventsForApplication(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accountInfo: StateFlow<com.pranay.jobtracker.data.AccountInfo?> = accountRepository.activeAccountIdFlow
        .filterNotNull()
        .flatMapLatest { accountId ->
            accountDao.getActiveAccounts().map { list -> list.find { it.accountId == accountId } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}
