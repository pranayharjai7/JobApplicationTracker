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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.pranay.jobtracker.data.ApplicationStage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class TimeFilter(val label: String, val days: Int?) {
    ALL("All", null),
    ONE_WEEK("1W", 7),
    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180),
    ONE_YEAR("1Y", 365)
}

private data class FilterState(
    val accountId: String,
    val selected: Set<String>,
    val groups: Map<String, List<String>>,
    val tFilter: TimeFilter,
    val selectedStages: Set<ApplicationStage>
)

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

    private fun normalizeCompany(name: String): String {
        return name.replace(Regex("(?i)\\b(llc|inc|corp|corporation|ltd|limited)\\b.*"), "")
            .trim()
    }

    val companyGroups: StateFlow<Map<String, List<String>>> = activeAccountFlow
        .filterNotNull()
        .flatMapLatest { accountId ->
            repository.getDistinctCompanies(accountId).map { rawList ->
                rawList.groupBy { normalizeCompany(it) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val selectedCompanies = MutableStateFlow<Set<String>>(emptySet())
    val timeFilter = MutableStateFlow(TimeFilter.ALL)
    val selectedStages = MutableStateFlow<Set<ApplicationStage>>(emptySet())

    val applications: StateFlow<List<JobApplication>> = combine(
        activeAccountFlow.filterNotNull(),
        selectedCompanies,
        companyGroups,
        timeFilter,
        selectedStages
    ) { accountId, selected, groups, tFilter, stages ->
        FilterState(accountId, selected, groups, tFilter, stages)
    }.flatMapLatest { state ->
        val rawFlow = if (state.selected.isEmpty()) {
            repository.getAllApplications(state.accountId)
        } else {
            val rawCompaniesToFetch = state.selected.flatMap { state.groups[it] ?: listOf(it) }
            repository.getApplicationsByCompanies(state.accountId, rawCompaniesToFetch)
        }
        
        rawFlow.map { list ->
            // Filter by stage
            val stageFiltered = if (state.selectedStages.isEmpty()) list 
                else list.filter { app -> state.selectedStages.map { it.name }.contains(app.stage) }
                
            // Filter by time
            if (state.tFilter.days == null) return@map stageFiltered
            val cutoff = Instant.now().minusSeconds(state.tFilter.days * 86400L).toEpochMilli()
            stageFiltered.filter {
                val time = if (it.createdAt > 0L) it.createdAt else parseLegacyDate(it.dateApplied)
                time >= cutoff 
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val heatmapData: StateFlow<Map<LocalDate, Int>> = combine(
        activeAccountFlow.filterNotNull(),
        timeFilter
    ) { accountId, tFilter ->
        Pair(accountId, tFilter)
    }.flatMapLatest { (accountId, tFilter) ->
        repository.getAllApplications(accountId).map { list ->
            val daysToKeep = tFilter.days ?: 365
            val cutoff = LocalDate.now().minusDays(daysToKeep.toLong())
            list.mapNotNull {
                val time = if (it.createdAt > 0L) it.createdAt else parseLegacyDate(it.dateApplied)
                val date = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate()
                if (date.isBefore(cutoff)) null else date
            }.groupingBy { it }.eachCount()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

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

    fun toggleCompanyFilter(normalizedCompany: String) {
        val current = selectedCompanies.value.toMutableSet()
        if (current.contains(normalizedCompany)) {
            current.remove(normalizedCompany)
        } else {
            current.add(normalizedCompany)
        }
        selectedCompanies.value = current
    }

    fun clearCompanyFilters() {
        selectedCompanies.value = emptySet()
    }

    fun setTimeFilter(filter: TimeFilter) {
        timeFilter.value = filter
    }

    fun toggleStageFilter(stage: ApplicationStage) {
        val current = selectedStages.value
        selectedStages.value = if (current.contains(stage)) current - stage else current + stage
    }

    fun clearStageFilters() {
        selectedStages.value = emptySet()
    }

    private fun parseLegacyDate(dateStr: String): Long {
        if (dateStr.isBlank() || dateStr == "Recent") return System.currentTimeMillis()
        val currentYear = LocalDate.now().year
        return runCatching {
            val cleanStr = dateStr.replace(Regex("(?i)st|nd|rd|th"), "")
            val format = java.text.SimpleDateFormat("EEE, d MMM yyyy", java.util.Locale.ENGLISH)
            format.parse("$cleanStr $currentYear")?.time
        }.getOrNull() ?: runCatching {
            val format2 = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.ENGLISH)
            format2.parse("$dateStr $currentYear")?.time
        }.getOrNull() ?: System.currentTimeMillis()
    }
}
