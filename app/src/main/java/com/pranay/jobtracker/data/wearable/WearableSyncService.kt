package com.pranay.jobtracker.data.wearable

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.gson.Gson
import com.pranay.jobtracker.data.JobApplicationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WearableSyncService : WearableListenerService() {

    @Inject
    lateinit var repository: JobApplicationRepository

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/jobtracker/update_status") {
            try {
                val json = String(messageEvent.data)
                val payload = gson.fromJson(json, StatusUpdatePayload::class.java)

                scope.launch {
                    val app = repository.getApplicationByIdStandalone(payload.applicationId)
                    if (app != null) {
                        val updatedApp = app.copy(
                            stage = payload.newStatus,
                            lastUpdatedAt = System.currentTimeMillis()
                        )
                        repository.updateApplication(updatedApp)
                        Log.d("WearableSyncService", "Updated application ${app.id} to status ${payload.newStatus}")
                    }
                }
            } catch (e: Exception) {
                Log.e("WearableSyncService", "Failed to process message", e)
            }
        } else {
            super.onMessageReceived(messageEvent)
        }
    }
}
