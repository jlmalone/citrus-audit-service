package com.citrus.audit.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ComplianceReport(
    val id: String,
    val reportType: ReportType,
    val generatedAt: Instant,
    val periodStart: Instant,
    val periodEnd: Instant,
    val summary: ComplianceSummary,
    val findings: List<ComplianceFinding> = emptyList()
)

@Serializable
enum class ReportType {
    SOC2_TYPE1,
    SOC2_TYPE2,
    GDPR_COMPLIANCE,
    DATA_RETENTION,
    ACCESS_REVIEW,
    SECURITY_AUDIT
}

@Serializable
data class ComplianceSummary(
    val totalEvents: Long,
    val totalUsers: Long,
    val totalAccesses: Long,
    val failedAccessAttempts: Long,
    val dataExports: Long,
    val dataErasures: Long,
    val complianceScore: Double // 0.0 to 100.0
)

@Serializable
data class ComplianceFinding(
    val severity: Severity,
    val category: String,
    val description: String,
    val affectedRecords: Long,
    val recommendation: String
)

@Serializable
enum class Severity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
