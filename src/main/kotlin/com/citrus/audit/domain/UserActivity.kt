package com.citrus.audit.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class UserActivity(
    val id: String,
    val userId: String,
    val sessionId: String,
    val activityType: ActivityType,
    val timestamp: Instant,
    val duration: Long? = null, // milliseconds
    val details: String? = null
)

@Serializable
enum class ActivityType {
    PAGE_VIEW,
    BUTTON_CLICK,
    FORM_SUBMIT,
    FILE_UPLOAD,
    FILE_DOWNLOAD,
    API_CALL,
    SEARCH,
    FILTER,
    SORT,
    EXPORT
}
