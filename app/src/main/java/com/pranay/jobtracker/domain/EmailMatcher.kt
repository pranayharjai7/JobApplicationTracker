package com.pranay.jobtracker.domain

import com.pranay.jobtracker.data.JobApplication

object EmailMatcher {

    fun normalize(text: String): String =
        text.lowercase()
            .trim()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun findMatch(
        parsedInfo: ParsedEmailInfo,
        candidates: List<JobApplication>
    ): JobApplication? {
        if (parsedInfo.companyName.isBlank() || parsedInfo.jobTitle.isBlank()) return null
        val normCompany = normalize(parsedInfo.companyName)
        val normTitle   = normalize(parsedInfo.jobTitle)
        return candidates.firstOrNull { app ->
            normalize(app.companyName) == normCompany && normalize(app.jobTitle) == normTitle
        }
    }
}
