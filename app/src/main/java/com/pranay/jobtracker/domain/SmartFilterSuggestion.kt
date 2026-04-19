package com.pranay.jobtracker.domain

import com.pranay.jobtracker.data.ApplicationStage
import com.pranay.jobtracker.ui.TimeFilter

data class SmartFilterSuggestion(
    val label: String,
    val rationale: String,
    val timeFilter: TimeFilter?,
    val stages: List<ApplicationStage>?,
    val companies: List<String>?
)
