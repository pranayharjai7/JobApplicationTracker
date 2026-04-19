package com.pranay.jobtracker.data

enum class ApplicationStage(val label: String) {
    APPLIED("Applied"),
    IN_REVIEW("In Review"),
    ASSESSMENT("Assessment"),
    INTERVIEW("Interview"),
    OFFER("Offer"),
    REJECTED("Rejected"),
    WITHDRAWN("Withdrawn")
}
