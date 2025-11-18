package com.citrus.audit.services

import com.citrus.audit.database.AuditEvents
import com.citrus.audit.database.DatabaseFactory.dbQuery
import com.citrus.audit.models.*
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class ComplianceService {

    /**
     * Generate SOC 2 compliance report
     */
    suspend fun generateSoc2Report(
        startTime: Instant,
        endTime: Instant
    ): ComplianceReport = dbQuery {
        val events = AuditEvents.selectAll()
            .where { (AuditEvents.timestamp greaterEq startTime) and (AuditEvents.timestamp lessEq endTime) }
            .toList()

        val totalEvents = events.size
        val criticalEvents = events.count { it[AuditEvents.severity] == EventSeverity.CRITICAL.name }
        val failedActions = events.count { !it[AuditEvents.success] }
        val uniqueUsers = events.mapNotNull { it[AuditEvents.userId] }.distinct().size
        val dataAccessEvents = events.count {
            it[AuditEvents.eventType] == EventType.RESOURCE_ACCESSED.name
        }
        val dataModificationEvents = events.count {
            it[AuditEvents.eventType] in listOf(
                EventType.RESOURCE_UPDATED.name,
                EventType.RESOURCE_DELETED.name
            )
        }
        val dataExportEvents = events.count {
            it[AuditEvents.eventType] == EventType.DATA_EXPORTED.name
        }

        val findings = mutableListOf<ComplianceFinding>()

        // Check for unauthorized access attempts
        val unauthorizedAttempts = events.count {
            !it[AuditEvents.success] && it[AuditEvents.eventType] == EventType.RESOURCE_ACCESSED.name
        }
        if (unauthorizedAttempts > 0) {
            findings.add(
                ComplianceFinding(
                    severity = "WARNING",
                    category = "Access Control",
                    description = "Unauthorized access attempts detected",
                    count = unauthorizedAttempts,
                    recommendation = "Review access control policies and investigate failed access attempts"
                )
            )
        }

        // Check for critical errors
        if (criticalEvents > 0) {
            findings.add(
                ComplianceFinding(
                    severity = "CRITICAL",
                    category = "System Integrity",
                    description = "Critical security events detected",
                    count = criticalEvents,
                    recommendation = "Immediate investigation required for critical events"
                )
            )
        }

        // Check for excessive data exports
        val dataExportThreshold = totalEvents * 0.05 // 5% threshold
        if (dataExportEvents > dataExportThreshold) {
            findings.add(
                ComplianceFinding(
                    severity = "INFO",
                    category = "Data Protection",
                    description = "High volume of data export operations",
                    count = dataExportEvents,
                    recommendation = "Review data export policies and ensure proper authorization"
                )
            )
        }

        ComplianceReport(
            id = UUID.randomUUID().toString(),
            reportType = ComplianceType.SOC2.name,
            generatedAt = Instant.now().toString(),
            periodStart = startTime.toString(),
            periodEnd = endTime.toString(),
            totalEvents = totalEvents,
            criticalEvents = criticalEvents,
            failedActions = failedActions,
            uniqueUsers = uniqueUsers,
            dataAccessEvents = dataAccessEvents,
            dataModificationEvents = dataModificationEvents,
            dataExportEvents = dataExportEvents,
            findings = findings,
            summary = "SOC 2 compliance report for period ${startTime} to ${endTime}. " +
                    "Total events: $totalEvents, Critical events: $criticalEvents, " +
                    "Unique users: $uniqueUsers, Findings: ${findings.size}"
        )
    }

    /**
     * Generate GDPR compliance report
     */
    suspend fun generateGdprReport(
        startTime: Instant,
        endTime: Instant
    ): ComplianceReport = dbQuery {
        val events = AuditEvents.selectAll()
            .where { (AuditEvents.timestamp greaterEq startTime) and (AuditEvents.timestamp lessEq endTime) }
            .toList()

        val totalEvents = events.size
        val criticalEvents = events.count { it[AuditEvents.severity] == EventSeverity.CRITICAL.name }
        val failedActions = events.count { !it[AuditEvents.success] }
        val uniqueUsers = events.mapNotNull { it[AuditEvents.userId] }.distinct().size
        val dataAccessEvents = events.count {
            it[AuditEvents.eventType] == EventType.RESOURCE_ACCESSED.name
        }
        val dataModificationEvents = events.count {
            it[AuditEvents.eventType] in listOf(
                EventType.RESOURCE_UPDATED.name,
                EventType.USER_UPDATED.name
            )
        }
        val dataExportEvents = events.count {
            it[AuditEvents.eventType] == EventType.DATA_EXPORTED.name
        }

        val findings = mutableListOf<ComplianceFinding>()

        // GDPR-specific checks
        val erasureRequests = events.count {
            it[AuditEvents.eventType] == EventType.GDPR_ERASURE_REQUESTED.name
        }
        val erasureCompleted = events.count {
            it[AuditEvents.eventType] == EventType.GDPR_ERASURE_COMPLETED.name
        }

        if (erasureRequests > erasureCompleted) {
            findings.add(
                ComplianceFinding(
                    severity = "WARNING",
                    category = "Right to Erasure",
                    description = "Pending GDPR erasure requests",
                    count = erasureRequests - erasureCompleted,
                    recommendation = "Complete pending GDPR erasure requests within 30 days"
                )
            )
        }

        // Check for data breach indicators
        val suspiciousActivity = events.count {
            !it[AuditEvents.success] && it[AuditEvents.severity] in listOf(
                EventSeverity.CRITICAL.name,
                EventSeverity.ERROR.name
            )
        }
        if (suspiciousActivity > 10) {
            findings.add(
                ComplianceFinding(
                    severity = "CRITICAL",
                    category = "Data Breach Prevention",
                    description = "Multiple failed critical operations detected",
                    count = suspiciousActivity,
                    recommendation = "Investigate potential data breach within 72 hours as per GDPR requirements"
                )
            )
        }

        // Check data access patterns
        val excessiveAccessUsers = events
            .filter { it[AuditEvents.eventType] == EventType.RESOURCE_ACCESSED.name }
            .groupBy { it[AuditEvents.userId] }
            .filter { it.value.size > 100 }
            .keys.size

        if (excessiveAccessUsers > 0) {
            findings.add(
                ComplianceFinding(
                    severity = "INFO",
                    category = "Data Minimization",
                    description = "Users with excessive data access",
                    count = excessiveAccessUsers,
                    recommendation = "Review data access patterns to ensure compliance with data minimization principle"
                )
            )
        }

        ComplianceReport(
            id = UUID.randomUUID().toString(),
            reportType = ComplianceType.GDPR.name,
            generatedAt = Instant.now().toString(),
            periodStart = startTime.toString(),
            periodEnd = endTime.toString(),
            totalEvents = totalEvents,
            criticalEvents = criticalEvents,
            failedActions = failedActions,
            uniqueUsers = uniqueUsers,
            dataAccessEvents = dataAccessEvents,
            dataModificationEvents = dataModificationEvents,
            dataExportEvents = dataExportEvents,
            findings = findings,
            summary = "GDPR compliance report for period ${startTime} to ${endTime}. " +
                    "Total events: $totalEvents, Erasure requests: $erasureRequests, " +
                    "Erasure completed: $erasureCompleted, Findings: ${findings.size}"
        )
    }

    /**
     * Get compliance statistics for a time period
     */
    suspend fun getComplianceStats(days: Int = 30): Map<String, Any> = dbQuery {
        val endTime = Instant.now()
        val startTime = endTime.minus(days.toLong(), ChronoUnit.DAYS)

        val events = AuditEvents.selectAll()
            .where { AuditEvents.timestamp greaterEq startTime }
            .toList()

        mapOf(
            "totalEvents" to events.size,
            "criticalEvents" to events.count { it[AuditEvents.severity] == EventSeverity.CRITICAL.name },
            "failedActions" to events.count { !it[AuditEvents.success] },
            "uniqueUsers" to events.mapNotNull { it[AuditEvents.userId] }.distinct().size,
            "eventsByType" to events.groupBy { it[AuditEvents.eventType] }
                .mapValues { it.value.size },
            "eventsBySeverity" to events.groupBy { it[AuditEvents.severity] }
                .mapValues { it.value.size }
        )
    }
}
