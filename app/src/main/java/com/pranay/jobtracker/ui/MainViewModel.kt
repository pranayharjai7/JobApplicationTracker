package com.pranay.jobtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranay.jobtracker.data.JobApplication
import com.pranay.jobtracker.data.JobApplicationRepository
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
import com.pranay.jobtracker.domain.SmartFilterSuggestion
import com.pranay.jobtracker.domain.ApplicationJourney
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
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
    val accountRepository: com.pranay.jobtracker.data.AccountRepository,
    private val eventRepository: com.pranay.jobtracker.data.EmailEventRepository,
    private val aiProviderFactory: com.pranay.jobtracker.domain.ai.AIProviderFactory
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
    
    val aiSmartFilters = MutableStateFlow<List<SmartFilterSuggestion>?>(null)
    val isFetchingAiFilters = MutableStateFlow(false)
    
    val isJourneyModeEnabled = MutableStateFlow(false)

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

    val applicationJourneys: StateFlow<List<ApplicationJourney>> = combine(
        applications,
        activeAccountFlow.filterNotNull().flatMapLatest { accountId ->
            eventRepository.getAllEventsForAccount(accountId)
        }
    ) { apps, events ->
        apps.map { app ->
            ApplicationJourney(
                application = app,
                timeline = events.filter { it.jobApplicationId == app.id }
            )
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

    fun clearAllFilters() {
        selectedCompanies.value = emptySet()
        timeFilter.value = TimeFilter.ALL
        selectedStages.value = emptySet()
    }

    fun applySmartFilter(suggestion: SmartFilterSuggestion) {
        if (suggestion.timeFilter != null) timeFilter.value = suggestion.timeFilter
        selectedStages.value = suggestion.stages?.toSet() ?: emptySet()
        selectedCompanies.value = suggestion.companies?.toSet() ?: emptySet()
    }

    fun fetchSmartFilters() {
        if (aiSmartFilters.value != null || isFetchingAiFilters.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isFetchingAiFilters.value = true
            try {
                val prompt = """
                    You are an intelligent Job Application Assistant tracking the user's dashboard.
                    Suggest exactly 3 "Smart Filters" the user might want to apply right now. Example: "Needs Follow Up" (IN_REVIEW, 1Month), "Recent Rejections", or "Active Interviews".
                    Return a JSON array of objects matching this exact structure:
                    [
                      {
                        "label": "Short Actionable Title (e.g. Follow up on Interviews)",
                        "rationale": "Why this filter helps.",
                        "timeFilter": "ONE_WEEK | ONE_MONTH | THREE_MONTHS | SIX_MONTHS | ONE_YEAR | ALL",
                        "stages": ["INTERVIEW"],  
                        "companies": []
                      }
                    ]
                    Only return the raw JSON Array block. No markdown.
                """.trimIndent()
                
                val provider = aiProviderFactory.getProvider()
                var text = provider.generateContent(prompt)
                
                if (text.contains("[")) {
                     text = text.substring(text.indexOf("["), text.lastIndexOf("]") + 1)
                }
                
                val listType = object : TypeToken<List<SmartFilterSuggestionDto>>() {}.type
                val dtos: List<SmartFilterSuggestionDto> = Gson().fromJson(text as String, listType)
                
                val mapped = dtos.map { dto ->
                     SmartFilterSuggestion(
                         label = dto.label,
                         rationale = dto.rationale,
                         timeFilter = runCatching { TimeFilter.valueOf(dto.timeFilter ?: "ALL") }.getOrNull(),
                         stages = dto.stages?.mapNotNull { s -> runCatching { ApplicationStage.valueOf(s) }.getOrNull() },
                         companies = dto.companies
                     )
                }
                aiSmartFilters.value = mapped
            } catch(e: Exception) {
                e.printStackTrace()
            } finally {
                isFetchingAiFilters.value = false
            }
        }
    }

    private data class SmartFilterSuggestionDto(
        val label: String,
        val rationale: String,
        val timeFilter: String?,
        val stages: List<String>?,
        val companies: List<String>?
    )

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
