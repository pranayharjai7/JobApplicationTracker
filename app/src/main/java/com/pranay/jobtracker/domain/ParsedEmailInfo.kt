package com.pranay.jobtracker.domain

data class ParsedEmailInfo(
    val sourceEmailId: String,
    val companyName: String,
    val jobTitle: String,
    val status: String,
    val dateStr: String,
    val dateEpochMs: Long,      // parsed from dateStr; 0 if unparseable
    val recruiterEmail: String?,
    val snippet: String         // the ≤500-char body passed to Gemini
)
