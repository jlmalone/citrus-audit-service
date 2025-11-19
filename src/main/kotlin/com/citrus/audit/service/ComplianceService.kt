package com.citrus.audit.service

import com.citrus.audit.domain.*
import com.citrus.audit.repository.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

class ComplianceService(
    private val reportRepository: ComplianceReportRepository = ComplianceReportRepository(),
    private val auditRepository: AuditEventRepository = AuditEventRepository(),
    private val accessLogRepository: AccessLogRepository = AccessLogRepository()
) {

    fun generateComplianceReport(
        reportType: ReportType,
        periodStart: Instant,
        periodEnd: Instant
    ): ComplianceReport {
        val auditEvents = auditRepository.findByTimeRange(periodStart, periodEnd)
        val failedAccesses = accessLogRepository.countFailedAttempts(periodStart, periodEnd)

        // Calculate metrics
        val totalEvents = auditEvents.size.toLong()
        val uniqueUsers = auditEvents.map { it.userId }.distinct().size.toLong()
        val dataExports = auditEvents.count { it.eventType == EventType.DATA_EXPORTED }.toLong()
        val dataErasures = auditEvents.count { it.eventType == EventType.DATA_ERASED }.toLong()

        // Generate findings based on report type
        val findings = when (reportType) {
            ReportType.SOC2_TYPE1, ReportType.SOC2_TYPE2 -> generateSOC2Findings(auditEvents, failedAccesses)
            ReportType.GDPR_COMPLIANCE -> generateGDPRFindings(auditEvents, dataExports, dataErasures)
            ReportType.DATA_RETENTION -> generateDataRetentionFindings()
            ReportType.ACCESS_REVIEW -> generateAccessReviewFindings(failedAccesses)
            ReportType.SECURITY_AUDIT -> generateSecurityAuditFindings(auditEvents, failedAccesses)
        }

        // Calculate compliance score
        val complianceScore = calculateComplianceScore(findings)

        val summary = ComplianceSummary(
            totalEvents = totalEvents,
            totalUsers = uniqueUsers,
            totalAccesses = totalEvents,
            failedAccessAttempts = failedAccesses,
            dataExports = dataExports,
            dataErasures = dataErasures,
            complianceScore = complianceScore
        )

        val report = ComplianceReport(
            id = UUID.randomUUID().toString(),
            reportType = reportType,
            generatedAt = Clock.System.now(),
            periodStart = periodStart,
            periodEnd = periodEnd,
            summary = summary,
            findings = findings
        )

        return reportRepository.create(report)
    }

    fun getReportById(id: String): ComplianceReport? {
        return reportRepository.findById(id)
    }

    fun getReportsByType(reportType: ReportType, limit: Int = 50): List<ComplianceReport> {
        return reportRepository.findByReportType(reportType, limit)
    }

    fun getAllReports(limit: Int = 50): List<ComplianceReport> {
        return reportRepository.findAll(limit)
    }

    private fun generateSOC2Findings(events: List<AuditEvent>, failedAccesses: Long): List<ComplianceFinding> {
        val findings = mutableListOf<ComplianceFinding>()

        if (failedAccesses > 100) {
            findings.add(ComplianceFinding(
                severity = Severity.HIGH,
                category = "Access Control",
                description = "High number of failed access attempts detected ($failedAccesses)",
                affectedRecords = failedAccesses,
                recommendation = "Review and strengthen authentication mechanisms"
            ))
        }

        val permissionChanges = events.count {
            it.eventType == EventType.PERMISSION_GRANTED || it.eventType == EventType.PERMISSION_REVOKED
        }
        if (permissionChanges > 0) {
            findings.add(ComplianceFinding(
                severity = Severity.LOW,
                category = "Permission Management",
                description = "$permissionChanges permission changes recorded",
                affectedRecords = permissionChanges.toLong(),
                recommendation = "Ensure all permission changes are properly documented and approved"
            ))
        }

        return findings
    }

    private fun generateGDPRFindings(events: List<AuditEvent>, exports: Long, erasures: Long): List<ComplianceFinding> {
        val findings = mutableListOf<ComplianceFinding>()

        findings.add(ComplianceFinding(
            severity = Severity.LOW,
            category = "Data Portability",
            description = "$exports data export requests processed",
            affectedRecords = exports,
            recommendation = "Ensure all data exports comply with GDPR requirements"
        ))

        findings.add(ComplianceFinding(
            severity = Severity.LOW,
            category = "Right to Erasure",
            description = "$erasures data erasure requests processed",
            affectedRecords = erasures,
            recommendation = "Verify all erasures are complete and irreversible"
        ))

        return findings
    }

    private fun generateDataRetentionFindings(): List<ComplianceFinding> {
        return listOf(
            ComplianceFinding(
                severity = Severity.MEDIUM,
                category = "Data Retention",
                description = "Review data retention policies for compliance",
                affectedRecords = 0,
                recommendation = "Ensure policies align with legal requirements"
            )
        )
    }

    private fun generateAccessReviewFindings(failedAccesses: Long): List<ComplianceFinding> {
        val findings = mutableListOf<ComplianceFinding>()

        if (failedAccesses > 50) {
            findings.add(ComplianceFinding(
                severity = Severity.MEDIUM,
                category = "Access Review",
                description = "Multiple failed access attempts detected",
                affectedRecords = failedAccesses,
                recommendation = "Investigate potential security threats"
            ))
        }

        return findings
    }

    private fun generateSecurityAuditFindings(events: List<AuditEvent>, failedAccesses: Long): List<ComplianceFinding> {
        val findings = mutableListOf<ComplianceFinding>()

        val deletionEvents = events.count { it.eventType == EventType.RESOURCE_DELETED || it.eventType == EventType.USER_DELETED }
        if (deletionEvents > 10) {
            findings.add(ComplianceFinding(
                severity = Severity.MEDIUM,
                category = "Data Security",
                description = "$deletionEvents deletion events recorded",
                affectedRecords = deletionEvents.toLong(),
                recommendation = "Review deletion patterns for anomalies"
            ))
        }

        if (failedAccesses > 200) {
            findings.add(ComplianceFinding(
                severity = Severity.CRITICAL,
                category = "Security Threat",
                description = "Critical: Very high number of failed access attempts",
                affectedRecords = failedAccesses,
                recommendation = "Immediate investigation required for potential breach"
            ))
        }

        return findings
    }

    private fun calculateComplianceScore(findings: List<ComplianceFinding>): Double {
        if (findings.isEmpty()) return 100.0

        val penalties = findings.sumOf { finding ->
            when (finding.severity) {
                Severity.LOW -> 2.0
                Severity.MEDIUM -> 5.0
                Severity.HIGH -> 10.0
                Severity.CRITICAL -> 25.0
            }
        }

        return maxOf(0.0, 100.0 - penalties)
    }
}
