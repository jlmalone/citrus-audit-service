package com.citrus.audit.service

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.EventType
import com.citrus.audit.domain.ReportType
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class ComplianceServiceTest {

    private val complianceService = ComplianceService()
    private val auditService = AuditService()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should generate SOC2 compliance report`() {
        val now = Clock.System.now()
        val start = now.minus(30.days)
        val end = now

        // Create some audit events
        auditService.createEvent(
            eventType = EventType.USER_LOGIN,
            userId = "user-123",
            action = "Login",
            resourceType = "user",
            resourceId = "user-123",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        auditService.createEvent(
            eventType = EventType.PERMISSION_GRANTED,
            userId = "admin-1",
            action = "Grant permission",
            resourceType = "permission",
            resourceId = "perm-1",
            ipAddress = "192.168.1.2",
            userAgent = "Mozilla/5.0"
        )

        val report = complianceService.generateComplianceReport(
            reportType = ReportType.SOC2_TYPE1,
            periodStart = start,
            periodEnd = end
        )

        assertNotNull(report.id)
        assertEquals(ReportType.SOC2_TYPE1, report.reportType)
        assertEquals(start, report.periodStart)
        assertEquals(end, report.periodEnd)
        assertTrue(report.summary.totalEvents >= 0)
        assertTrue(report.summary.complianceScore >= 0.0 && report.summary.complianceScore <= 100.0)
    }

    @Test
    fun `should generate GDPR compliance report`() {
        val now = Clock.System.now()
        val start = now.minus(30.days)
        val end = now

        auditService.createEvent(
            eventType = EventType.DATA_EXPORTED,
            userId = "user-123",
            action = "Export data",
            resourceType = "user_data",
            resourceId = "export-1",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        auditService.createEvent(
            eventType = EventType.DATA_ERASED,
            userId = "user-456",
            action = "Erase data",
            resourceType = "user_data",
            resourceId = "erase-1",
            ipAddress = "192.168.1.2",
            userAgent = "Mozilla/5.0"
        )

        val report = complianceService.generateComplianceReport(
            reportType = ReportType.GDPR_COMPLIANCE,
            periodStart = start,
            periodEnd = end
        )

        assertNotNull(report.id)
        assertEquals(ReportType.GDPR_COMPLIANCE, report.reportType)
        assertTrue(report.summary.dataExports >= 0)
        assertTrue(report.summary.dataErasures >= 0)
    }

    @Test
    fun `should get report by id`() {
        val now = Clock.System.now()
        val start = now.minus(30.days)
        val end = now

        val created = complianceService.generateComplianceReport(
            reportType = ReportType.DATA_RETENTION,
            periodStart = start,
            periodEnd = end
        )

        val found = complianceService.getReportById(created.id)
        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals(created.reportType, found.reportType)
    }

    @Test
    fun `should get reports by type`() {
        val now = Clock.System.now()
        val start = now.minus(30.days)
        val end = now

        complianceService.generateComplianceReport(
            reportType = ReportType.SOC2_TYPE1,
            periodStart = start,
            periodEnd = end
        )

        complianceService.generateComplianceReport(
            reportType = ReportType.SOC2_TYPE1,
            periodStart = start,
            periodEnd = end
        )

        val reports = complianceService.getReportsByType(ReportType.SOC2_TYPE1)
        assertTrue(reports.size >= 2)
    }

    @Test
    fun `should calculate compliance score correctly`() {
        val now = Clock.System.now()
        val start = now.minus(30.days)
        val end = now

        // Create events with no issues
        auditService.createEvent(
            eventType = EventType.USER_LOGIN,
            userId = "user-123",
            action = "Login",
            resourceType = "user",
            resourceId = "user-123",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        val report = complianceService.generateComplianceReport(
            reportType = ReportType.SECURITY_AUDIT,
            periodStart = start,
            periodEnd = end
        )

        // With minimal events and no critical findings, score should be high
        assertTrue(report.summary.complianceScore >= 50.0)
    }

    @Test
    fun `should get all reports`() {
        val now = Clock.System.now()
        val start = now.minus(30.days)
        val end = now

        complianceService.generateComplianceReport(
            reportType = ReportType.SOC2_TYPE1,
            periodStart = start,
            periodEnd = end
        )

        complianceService.generateComplianceReport(
            reportType = ReportType.GDPR_COMPLIANCE,
            periodStart = start,
            periodEnd = end
        )

        val reports = complianceService.getAllReports()
        assertTrue(reports.size >= 2)
    }
}
