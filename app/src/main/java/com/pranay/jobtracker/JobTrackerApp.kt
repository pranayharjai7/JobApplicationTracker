package com.pranay.jobtracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import javax.inject.Inject
import com.pranay.jobtracker.data.wearable.PhoneWearableSyncManager

@HiltAndroidApp
class JobTrackerApp : Application() {

    @Inject
    lateinit var syncManager: PhoneWearableSyncManager

    override fun onCreate() {
        super.onCreate()
        syncManager.startSyncing(this)
    }
}
