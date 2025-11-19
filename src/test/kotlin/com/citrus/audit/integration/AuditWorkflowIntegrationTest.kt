package com.citrus.audit.integration

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.EventType
import com.citrus.audit.domain.ReportType
import com.citrus.audit.service.AuditService
import com.citrus.audit.service.ComplianceService
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class AuditWorkflowIntegrationTest {

    private val auditService = AuditService()
    private val complianceService = ComplianceService()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should complete full audit logging workflow`() {
        // Step 1: User logs in
        val loginEvent = auditService.createEvent(
            eventType = EventType.USER_LOGIN,
            userId = "user-integration-1",
            action = "User logged in",
            resourceType = "user",
            resourceId = "user-integration-1",
            ipAddress = "192.168.1.100",
            userAgent = "Mozilla/5.0"
        )
        assertNotNull(loginEvent)

        // Step 2: User creates a resource
        val createEvent = auditService.createEvent(
            eventType = EventType.RESOURCE_CREATED,
            userId = "user-integration-1",
            action = "Created document",
            resourceType = "document",
            resourceId = "doc-integration-1",
            ipAddress = "192.168.1.100",
            userAgent = "Mozilla/5.0"
        )
        assertNotNull(createEvent)

        // Step 3: User updates the resource
        val updateEvent = auditService.createEvent(
            eventType = EventType.RESOURCE_UPDATED,
            userId = "user-integration-1",
            action = "Updated document",
            resourceType = "document",
            resourceId = "doc-integration-1",
            ipAddress = "192.168.1.100",
            userAgent = "Mozilla/5.0"
        )
        assertNotNull(updateEvent)

        // Step 4: User exports data
        val exportEvent = auditService.createEvent(
            eventType = EventType.DATA_EXPORTED,
            userId = "user-integration-1",
            action = "Exported data",
            resourceType = "export",
            resourceId = "export-1",
            ipAddress = "192.168.1.100",
            userAgent = "Mozilla/5.0"
        )
        assertNotNull(exportEvent)

        // Step 5: User logs out
        val logoutEvent = auditService.createEvent(
            eventType = EventType.USER_LOGOUT,
            userId = "user-integration-1",
            action = "User logged out",
            resourceType = "user",
            resourceId = "user-integration-1",
            ipAddress = "192.168.1.100",
            userAgent = "Mozilla/5.0"
        )
        assertNotNull(logoutEvent)

        // Step 6: Verify all events were logged
        val userEvents = auditService.getUserEvents("user-integration-1")
        assertEquals(5, userEvents.size)

        // Step 7: Generate compliance report
        val now = Clock.System.now()
        val start = now.minus(1.days)

        val report = complianceService.generateComplianceReport(
            reportType = ReportType.GDPR_COMPLIANCE,
            periodStart = start,
            periodEnd = now
        )

        assertNotNull(report)
        assertTrue(report.summary.totalEvents >= 5)
        assertTrue(report.summary.dataExports >= 1)
    }

    @Test
    fun `should track user session lifecycle`() {
        val userId = "user-session-test"

        // Login
        auditService.createEvent(
            eventType = EventType.USER_LOGIN,
            userId = userId,
            action = "Login",
            resourceType = "user",
            resourceId = userId,
            ipAddress = "10.0.0.1",
            userAgent = "Chrome"
        )

        // Multiple activities
        repeat(10) { i ->
            auditService.createEvent(
                eventType = EventType.RESOURCE_READ,
                userId = userId,
                action = "Read resource $i",
                resourceType = "document",
                resourceId = "doc-$i",
                ipAddress = "10.0.0.1",
                userAgent = "Chrome"
            )
        }

        // Logout
        auditService.createEvent(
            eventType = EventType.USER_LOGOUT,
            userId = userId,
            action = "Logout",
            resourceType = "user",
            resourceId = userId,
            ipAddress = "10.0.0.1",
            userAgent = "Chrome"
        )

        val events = auditService.getUserEvents(userId, limit = 50)
        assertEquals(12, events.size)

        val loginCount = events.count { it.eventType == EventType.USER_LOGIN }
        val logoutCount = events.count { it.eventType == EventType.USER_LOGOUT }
        assertEquals(1, loginCount)
        assertEquals(1, logoutCount)
    }

    @Test
    fun `should handle multi-user concurrent audit logging`() {
        val users = listOf("user-1", "user-2", "user-3", "user-4", "user-5")

        users.forEach { userId ->
            repeat(5) { i ->
                auditService.createEvent(
                    eventType = EventType.RESOURCE_CREATED,
                    userId = userId,
                    action = "Action $i",
                    resourceType = "document",
                    resourceId = "$userId-doc-$i",
                    ipAddress = "192.168.1.1",
                    userAgent = "Mozilla/5.0"
                )
            }
        }

        users.forEach { userId ->
            val events = auditService.getUserEvents(userId)
            assertEquals(5, events.size)
        }
    }

    @Test
    fun `should generate multiple compliance reports`() {
        val now = Clock.System.now()
        val start = now.minus(30.days)

        // Create audit trail
        repeat(20) { i ->
            auditService.createEvent(
                eventType = EventType.RESOURCE_CREATED,
                userId = "user-compliance-$i",
                action = "Create resource",
                resourceType = "document",
                resourceId = "doc-$i",
                ipAddress = "192.168.1.1",
                userAgent = "Mozilla/5.0"
            )
        }

        // Generate different types of reports
        val soc2Report = complianceService.generateComplianceReport(
            reportType = ReportType.SOC2_TYPE1,
            periodStart = start,
            periodEnd = now
        )

        val gdprReport = complianceService.generateComplianceReport(
            reportType = ReportType.GDPR_COMPLIANCE,
            periodStart = start,
            periodEnd = now
        )

        val securityReport = complianceService.generateComplianceReport(
            reportType = ReportType.SECURITY_AUDIT,
            periodStart = start,
            periodEnd = now
        )

        assertNotNull(soc2Report)
        assertNotNull(gdprReport)
        assertNotNull(securityReport)

        assertTrue(soc2Report.summary.totalEvents >= 20)
        assertTrue(gdprReport.summary.totalEvents >= 20)
        assertTrue(securityReport.summary.totalEvents >= 20)
    }

    @Test
    fun `should maintain event ordering by timestamp`() {
        val userId = "user-ordering-test"

        // Create events
        val event1 = auditService.createEvent(
            eventType = EventType.USER_LOGIN,
            userId = userId,
            action = "First event",
            resourceType = "user",
            resourceId = userId,
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        Thread.sleep(10) // Small delay to ensure different timestamps

        val event2 = auditService.createEvent(
            eventType = EventType.RESOURCE_CREATED,
            userId = userId,
            action = "Second event",
            resourceType = "document",
            resourceId = "doc-1",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        Thread.sleep(10)

        val event3 = auditService.createEvent(
            eventType = EventType.USER_LOGOUT,
            userId = userId,
            action = "Third event",
            resourceType = "user",
            resourceId = userId,
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        val events = auditService.getUserEvents(userId)

        // Events should be returned in reverse chronological order (newest first)
        assertEquals(event3.id, events[0].id)
        assertEquals(event2.id, events[1].id)
        assertEquals(event1.id, events[2].id)
    }
}
