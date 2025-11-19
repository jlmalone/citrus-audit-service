package com.citrus.audit.repository

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.*
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class ComplianceReportRepositoryTest {

    private val repository = ComplianceReportRepository()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should create and find compliance report`() {
        val report = createTestReport("report-1", ReportType.SOC2_TYPE1)

        repository.create(report)
        val found = repository.findById("report-1")

        assertNotNull(found)
        assertEquals(report.id, found.id)
        assertEquals(report.reportType, found.reportType)
        assertEquals(report.summary.totalEvents, found.summary.totalEvents)
    }

    @Test
    fun `should find reports by type`() {
        val report1 = createTestReport("report-1", ReportType.SOC2_TYPE1)
        val report2 = createTestReport("report-2", ReportType.SOC2_TYPE1)
        val report3 = createTestReport("report-3", ReportType.GDPR_COMPLIANCE)

        repository.create(report1)
        repository.create(report2)
        repository.create(report3)

        val soc2Reports = repository.findByReportType(ReportType.SOC2_TYPE1)
        assertEquals(2, soc2Reports.size)
    }

    @Test
    fun `should find all reports`() {
        val report1 = createTestReport("report-1", ReportType.SOC2_TYPE1)
        val report2 = createTestReport("report-2", ReportType.GDPR_COMPLIANCE)
        val report3 = createTestReport("report-3", ReportType.DATA_RETENTION)

        repository.create(report1)
        repository.create(report2)
        repository.create(report3)

        val allReports = repository.findAll()
        assertEquals(3, allReports.size)
    }

    @Test
    fun `should return null for non-existent report`() {
        val found = repository.findById("non-existent")
        assertNull(found)
    }

    @Test
    fun `should store and retrieve compliance findings`() {
        val findings = listOf(
            ComplianceFinding(
                severity = Severity.HIGH,
                category = "Security",
                description = "Critical security issue",
                affectedRecords = 100,
                recommendation = "Fix immediately"
            ),
            ComplianceFinding(
                severity = Severity.LOW,
                category = "Documentation",
                description = "Missing docs",
                affectedRecords = 5,
                recommendation = "Update documentation"
            )
        )

        val report = createTestReport("report-with-findings", ReportType.SECURITY_AUDIT)
            .copy(findings = findings)

        repository.create(report)
        val found = repository.findById("report-with-findings")

        assertNotNull(found)
        assertEquals(2, found.findings.size)
        assertEquals(Severity.HIGH, found.findings[0].severity)
        assertEquals("Security", found.findings[0].category)
    }

    private fun createTestReport(id: String, reportType: ReportType): ComplianceReport {
        val now = Clock.System.now()
        val start = now.minus(30.days)

        return ComplianceReport(
            id = id,
            reportType = reportType,
            generatedAt = now,
            periodStart = start,
            periodEnd = now,
            summary = ComplianceSummary(
                totalEvents = 100,
                totalUsers = 50,
                totalAccesses = 200,
                failedAccessAttempts = 5,
                dataExports = 3,
                dataErasures = 1,
                complianceScore = 95.5
            ),
            findings = emptyList()
        )
    }
}
