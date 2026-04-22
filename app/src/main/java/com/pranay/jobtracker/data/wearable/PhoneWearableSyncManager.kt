package com.pranay.jobtracker.data.wearable

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.pranay.jobtracker.data.JobApplication
import com.pranay.jobtracker.data.ApplicationStage
import com.pranay.jobtracker.data.JobApplicationRepository
import com.pranay.jobtracker.data.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhoneWearableSyncManager @Inject constructor(
    private val repository: JobApplicationRepository,
    private val accountRepository: AccountRepository
) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun startSyncing(context: Context) {
        val dataClient = Wearable.getDataClient(context)

        scope.launch {
            accountRepository.activeAccountIdFlow
                .filterNotNull()
                .flatMapLatest { accountId ->
                    repository.getAllApplications(accountId)
                }
                .collectLatest { applications ->
                    val totalCount = applications.size
                    val inProgressCount = applications.count { 
                        it.stage == ApplicationStage.APPLIED.name || 
                        it.stage == ApplicationStage.IN_REVIEW.name || 
                        it.stage == ApplicationStage.ASSESSMENT.name 
                    }
                    val interviewsCount = applications.count { it.stage == ApplicationStage.INTERVIEW.name }
                    val offersCount = applications.count { it.stage == ApplicationStage.OFFER.name }
                    val rejectedCount = applications.count { it.stage == ApplicationStage.REJECTED.name }

                    val recentApps = applications
                        .sortedByDescending { it.lastUpdatedAt }
                        .take(20)
                        .map { app ->
                            ApplicationSyncModel(
                                id = app.id,
                                company = app.companyName,
                                role = app.jobTitle,
                                status = app.stage,
                                lastUpdate = app.lastUpdate,
                                appliedDate = app.dateApplied
                            )
                        }

                    val payload = SyncPayload(
                        totalCount = totalCount,
                        inProgressCount = inProgressCount,
                        interviewsCount = interviewsCount,
                        offersCount = offersCount,
                        rejectedCount = rejectedCount,
                        applications = recentApps
                    )

                    try {
                        val putDataMapReq = PutDataMapRequest.create("/jobtracker/sync")
                        putDataMapReq.dataMap.putString("payload", gson.toJson(payload))
                        // Add timestamp to ensure data item changes and triggers listeners even if payload looks same
                        putDataMapReq.dataMap.putLong("timestamp", System.currentTimeMillis())
                        
                        val request = putDataMapReq.asPutDataRequest()
                        request.setUrgent()
                        
                        dataClient.putDataItem(request)
                        Log.d("PhoneWearableSync", "Synced data to wearable")
                    } catch (e: Exception) {
                        Log.e("PhoneWearableSync", "Failed to sync data", e)
                    }
                }
        }
    }
}
