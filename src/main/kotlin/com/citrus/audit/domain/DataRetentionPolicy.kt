package com.citrus.audit.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DataRetentionPolicy(
    val id: String,
    val name: String,
    val description: String,
    val dataType: DataType,
    val retentionPeriodDays: Int,
    val autoDelete: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isActive: Boolean = true
)

@Serializable
enum class DataType {
    AUDIT_EVENTS,
    USER_ACTIVITIES,
    ACCESS_LOGS,
    COMPLIANCE_REPORTS,
    USER_DATA,
    TRANSACTION_DATA
}

@Serializable
data class GdprErasureRequest(
    val id: String,
    val userId: String,
    val requestedAt: Instant,
    val status: ErasureStatus,
    val completedAt: Instant? = null,
    val deletedRecords: Map<String, Long> = emptyMap()
)

@Serializable
enum class ErasureStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
