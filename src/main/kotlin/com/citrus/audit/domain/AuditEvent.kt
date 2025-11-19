package com.citrus.audit.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AuditEvent(
    val id: String,
    val timestamp: Instant,
    val eventType: EventType,
    val userId: String,
    val action: String,
    val resourceType: String,
    val resourceId: String,
    val ipAddress: String,
    val userAgent: String,
    val metadata: Map<String, String> = emptyMap(),
    val status: EventStatus = EventStatus.SUCCESS
)

@Serializable
enum class EventType {
    USER_LOGIN,
    USER_LOGOUT,
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    RESOURCE_CREATED,
    RESOURCE_READ,
    RESOURCE_UPDATED,
    RESOURCE_DELETED,
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,
    DATA_EXPORTED,
    DATA_ERASED,
    COMPLIANCE_REPORT_GENERATED
}

@Serializable
enum class EventStatus {
    SUCCESS,
    FAILURE,
    PARTIAL
}
