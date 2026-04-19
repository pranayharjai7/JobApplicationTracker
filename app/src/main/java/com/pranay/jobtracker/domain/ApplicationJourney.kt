package com.pranay.jobtracker.domain

import com.pranay.jobtracker.data.EmailEvent
import com.pranay.jobtracker.data.JobApplication

data class ApplicationJourney(
    val application: JobApplication,
    val timeline: List<EmailEvent>
)
