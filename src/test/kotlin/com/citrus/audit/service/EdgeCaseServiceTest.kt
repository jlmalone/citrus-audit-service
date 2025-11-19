package com.citrus.audit.service

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.EventType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class EdgeCaseServiceTest {

    private val auditService = AuditService()
    private val complianceService = ComplianceService()
    private val retentionService = DataRetentionService()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should handle empty time range queries`() {
        val now = Clock.System.now()
        val future = now.plus(1.days)

        val events = auditService.getEventsInTimeRange(now, future)
        assertNotNull(events)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `should handle queries with limit of zero`() {
        auditService.createEvent(
            eventType = EventType.USER_LOGIN,
            userId = "user-123",
            action = "Login",
            resourceType = "user",
            resourceId = "user-123",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        val events = auditService.getUserEvents("user-123", limit = 0)
        assertNotNull(events)
    }

    @Test
    fun `should handle very large limit values`() {
        auditService.createEvent(
            eventType = EventType.USER_LOGIN,
            userId = "user-123",
            action = "Login",
            resourceType = "user",
            resourceId = "user-123",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        val events = auditService.getUserEvents("user-123", limit = Int.MAX_VALUE)
        assertNotNull(events)
        assertEquals(1, events.size)
    }

    @Test
    fun `should handle events with empty metadata`() {
        val event = auditService.createEvent(
            eventType = EventType.USER_LOGIN,
            userId = "user-123",
            action = "Login",
            resourceType = "user",
            resourceId = "user-123",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0",
            metadata = emptyMap()
        )

        assertNotNull(event)
        assertTrue(event.metadata.isEmpty())
    }

    @Test
    fun `should handle events with large metadata`() {
        val largeMetadata = (1..100).associate { "key$it" to "value$it" }

        val event = auditService.createEvent(
            eventType = EventType.RESOURCE_CREATED,
            userId = "user-123",
            action = "Create with metadata",
            resourceType = "document",
            resourceId = "doc-1",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0",
            metadata = largeMetadata
        )

        assertNotNull(event)
        assertEquals(100, event.metadata.size)

        val found = auditService.getEventById(event.id)
        assertNotNull(found)
        assertEquals(100, found.metadata.size)
    }

    @Test
    fun `should handle compliance report with no events`() {
        val now = Clock.System.now()
        val start = now.minus(30.days)

        val report = complianceService.generateComplianceReport(
            reportType = com.citrus.audit.domain.ReportType.SOC2_TYPE1,
            periodStart = start,
            periodEnd = now
        )

        assertNotNull(report)
        assertEquals(0, report.summary.totalEvents)
        assertEquals(100.0, report.summary.complianceScore)
    }

    @Test
    fun `should handle very short time ranges`() {
        val now = Clock.System.now()
        val start = now.minus(1.hours)

        val report = complianceService.generateComplianceReport(
            reportType = com.citrus.audit.domain.ReportType.GDPR_COMPLIANCE,
            periodStart = start,
            periodEnd = now
        )

        assertNotNull(report)
    }

    @Test
    fun `should handle same start and end time`() {
        val now = Clock.System.now()

        val report = complianceService.generateComplianceReport(
            reportType = com.citrus.audit.domain.ReportType.ACCESS_REVIEW,
            periodStart = now,
            periodEnd = now
        )

        assertNotNull(report)
        assertEquals(0, report.summary.totalEvents)
    }

    @Test
    fun `should handle user with no events`() {
        val count = auditService.getUserEventCount("non-existent-user")
        assertEquals(0, count)

        val events = auditService.getUserEvents("non-existent-user")
        assertTrue(events.isEmpty())
    }

    @Test
    fun `should handle deletion of non-existent old data`() {
        val cutoff = Clock.System.now().minus(365.days)
        val deleted = auditService.deleteOldEvents(cutoff)
        assertEquals(0, deleted)
    }

    @Test
    fun `should handle multiple erasure requests for same user`() {
        val request1 = retentionService.requestGdprErasure("user-123")
        val request2 = retentionService.requestGdprErasure("user-123")

        assertNotNull(request1)
        assertNotNull(request2)

        val requests = retentionService.getUserErasureRequests("user-123")
        assertEquals(2, requests.size)
    }

    @Test
    fun `should handle special characters in user IDs`() {
        val event = auditService.createEvent(
            eventType = EventType.USER_LOGIN,
            userId = "user@email.com",
            action = "Login",
            resourceType = "user",
            resourceId = "user@email.com",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        assertNotNull(event)
        assertEquals("user@email.com", event.userId)

        val found = auditService.getUserEvents("user@email.com")
        assertEquals(1, found.size)
    }

    @Test
    fun `should handle very long strings in event fields`() {
        val longString = "a".repeat(1000)

        val event = auditService.createEvent(
            eventType = EventType.RESOURCE_CREATED,
            userId = "user-123",
            action = longString,
            resourceType = "document",
            resourceId = "doc-1",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        assertNotNull(event)
        val found = auditService.getEventById(event.id)
        assertNotNull(found)
        assertEquals(longString, found.action)
    }

    @Test
    fun `should handle Unicode characters in event data`() {
        val event = auditService.createEvent(
            eventType = EventType.RESOURCE_CREATED,
            userId = "用户-123",
            action = "创建文档",
            resourceType = "document",
            resourceId = "doc-1",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0 (中文)",
            metadata = mapOf("language" to "中文", "emoji" to "🍊")
        )

        assertNotNull(event)
        assertEquals("用户-123", event.userId)
        assertEquals("创建文档", event.action)
        assertEquals("🍊", event.metadata["emoji"])

        val found = auditService.getEventById(event.id)
        assertNotNull(found)
        assertEquals("中文", found.metadata["language"])
    }
}
