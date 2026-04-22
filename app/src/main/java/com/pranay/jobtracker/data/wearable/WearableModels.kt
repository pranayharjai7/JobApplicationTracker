package com.pranay.jobtracker.data.wearable

import com.google.gson.annotations.SerializedName

data class SyncPayload(
    @SerializedName("totalCount") val totalCount: Int,
    @SerializedName("inProgressCount") val inProgressCount: Int,
    @SerializedName("interviewsCount") val interviewsCount: Int,
    @SerializedName("offersCount") val offersCount: Int,
    @SerializedName("rejectedCount") val rejectedCount: Int,
    @SerializedName("applications") val applications: List<ApplicationSyncModel>
)

data class ApplicationSyncModel(
    @SerializedName("id") val id: Int,
    @SerializedName("company") val company: String,
    @SerializedName("role") val role: String,
    @SerializedName("status") val status: String,
    @SerializedName("lastUpdate") val lastUpdate: String,
    @SerializedName("appliedDate") val appliedDate: String
)

data class StatusUpdatePayload(
    @SerializedName("applicationId") val applicationId: Int,
    @SerializedName("newStatus") val newStatus: String
)
