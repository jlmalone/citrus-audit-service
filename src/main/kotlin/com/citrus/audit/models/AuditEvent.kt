package com.citrus.audit.models

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Core audit event types
 */
enum class EventType {
    USER_LOGIN,
    USER_LOGOUT,
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    RESOURCE_ACCESSED,
    RESOURCE_CREATED,
    RESOURCE_UPDATED,
    RESOURCE_DELETED,
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,
    PAYMENT_PROCESSED,
    PAYMENT_FAILED,
    DATA_EXPORTED,
    DATA_IMPORTED,
    COMPLIANCE_REPORT_GENERATED,
    GDPR_ERASURE_REQUESTED,
    GDPR_ERASURE_COMPLETED,
    RETENTION_POLICY_APPLIED
}

/**
 * Severity levels for audit events
 */
enum class EventSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Main audit event model
 */
data class AuditEvent(
    val id: UUID = UUID.randomUUID(),
    val eventType: EventType,
    val timestamp: Instant = Instant.now(),
    val userId: String?,
    val username: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val resourceType: String?,
    val resourceId: String?,
    val action: String,
    val description: String,
    val severity: EventSeverity = EventSeverity.INFO,
    val metadata: Map<String, String> = emptyMap(),
    val success: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Serializable version for API responses
 */
@Serializable
data class AuditEventDto(
    val id: String,
    val eventType: String,
    val timestamp: String,
    val userId: String?,
    val username: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val resourceType: String?,
    val resourceId: String?,
    val action: String,
    val description: String,
    val severity: String,
    val metadata: Map<String, String>,
    val success: Boolean,
    val errorMessage: String?
)

/**
 * User activity summary
 */
@Serializable
data class UserActivitySummary(
    val userId: String,
    val username: String?,
    val loginCount: Int,
    val lastLogin: String?,
    val resourceAccessCount: Int,
    val failedActionCount: Int,
    val mostRecentActions: List<AuditEventDto>
)

/**
 * Access log entry
 */
@Serializable
data class AccessLogEntry(
    val id: String,
    val timestamp: String,
    val userId: String?,
    val username: String?,
    val ipAddress: String?,
    val resourceType: String?,
    val resourceId: String?,
    val action: String,
    val success: Boolean
)

/**
 * Receipt for audit operations
 */
@Serializable
data class AuditReceipt(
    val receiptId: String,
    val eventId: String,
    val timestamp: String,
    val eventType: String,
    val userId: String?,
    val action: String,
    val checksum: String
)

/**
 * Compliance report types
 */
enum class ComplianceType {
    SOC2,
    GDPR,
    HIPAA,
    PCI_DSS
}

/**
 * Compliance report
 */
@Serializable
data class ComplianceReport(
    val id: String,
    val reportType: String,
    val generatedAt: String,
    val periodStart: String,
    val periodEnd: String,
    val totalEvents: Int,
    val criticalEvents: Int,
    val failedActions: Int,
    val uniqueUsers: Int,
    val dataAccessEvents: Int,
    val dataModificationEvents: Int,
    val dataExportEvents: Int,
    val findings: List<ComplianceFinding>,
    val summary: String
)

/**
 * Compliance finding
 */
@Serializable
data class ComplianceFinding(
    val severity: String,
    val category: String,
    val description: String,
    val count: Int,
    val recommendation: String?
)

/**
 * GDPR erasure request
 */
@Serializable
data class GdprErasureRequest(
    val userId: String,
    val requestedBy: String,
    val reason: String
)

/**
 * GDPR erasure response
 */
@Serializable
data class GdprErasureResponse(
    val requestId: String,
    val userId: String,
    val eventsErased: Int,
    val timestamp: String,
    val status: String
)

/**
 * Data retention policy
 */
@Serializable
data class RetentionPolicy(
    val id: String,
    val eventType: String,
    val retentionDays: Int,
    val autoDelete: Boolean,
    val description: String
)

/**
 * Query filters for audit events
 */
@Serializable
data class AuditQueryFilter(
    val userId: String? = null,
    val eventType: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val resourceType: String? = null,
    val resourceId: String? = null,
    val severity: String? = null,
    val successOnly: Boolean? = null,
    val limit: Int = 100,
    val offset: Int = 0
)
