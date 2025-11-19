package com.citrus.audit.security

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.EventType
import com.citrus.audit.service.AuditService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InputValidationTest {

    private val auditService = AuditService()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should handle SQL-like strings without injection`() {
        val sqlLikeInput = "'; DROP TABLE audit_events; --"

        val event = auditService.createEvent(
            eventType = EventType.RESOURCE_CREATED,
            userId = sqlLikeInput,
            action = "Test SQL injection prevention",
            resourceType = "document",
            resourceId = "doc-1",
            ipAddress = "192.168.1.1",
            userAgent = sqlLikeInput
        )

        assertNotNull(event)
        val found = auditService.getUserEvents(sqlLikeInput)
        assertTrue(found.isNotEmpty())
    }

    @Test
    fun `should handle XSS-like strings in event data`() {
        val xssLikeInput = "<script>alert('XSS')</script>"

        val event = auditService.createEvent(
            eventType = EventType.RESOURCE_CREATED,
            userId = "user-123",
            action = xssLikeInput,
            resourceType = "document",
            resourceId = "doc-1",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0",
            metadata = mapOf("description" to xssLikeInput)
        )

        assertNotNull(event)
        val found = auditService.getEventById(event.id)
        assertNotNull(found)
        assertTrue(found.action.contains("<script>"))
        assertTrue(found.metadata["description"]!!.contains("<script>"))
    }

    @Test
    fun `should handle command injection attempts`() {
        val commandInjection = "test && rm -rf /"

        val event = auditService.createEvent(
            eventType = EventType.RESOURCE_DELETED,
            userId = commandInjection,
            action = "Delete with malicious input",
            resourceType = "document",
            resourceId = commandInjection,
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        assertNotNull(event)
        assertTrue(event.userId.contains("&&"))
    }

    @Test
    fun `should handle path traversal attempts`() {
        val pathTraversal = "../../etc/passwd"

        val event = auditService.createEvent(
            eventType = EventType.RESOURCE_READ,
            userId = "user-123",
            action = "Read file",
            resourceType = "document",
            resourceId = pathTraversal,
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        assertNotNull(event)
        assertTrue(event.resourceId.contains("../"))
    }

    @Test
    fun `should handle very long input strings`() {
        val longInput = "A".repeat(10000)

        val event = auditService.createEvent(
            eventType = EventType.RESOURCE_CREATED,
            userId = "user-123",
            action = longInput,
            resourceType = "document",
            resourceId = "doc-1",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        assertNotNull(event)
        val found = auditService.getEventById(event.id)
        assertNotNull(found)
        assertTrue(found.action.length == 10000)
    }

    @Test
    fun `should handle null-byte injection attempts`() {
        val nullByteInput = "test\u0000malicious"

        val event = auditService.createEvent(
            eventType = EventType.RESOURCE_CREATED,
            userId = "user-123",
            action = nullByteInput,
            resourceType = "document",
            resourceId = "doc-1",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        assertNotNull(event)
    }

    @Test
    fun `should handle LDAP injection attempts`() {
        val ldapInjection = "*)(uid=*))(|(uid=*"

        val event = auditService.createEvent(
            eventType = EventType.USER_LOGIN,
            userId = ldapInjection,
            action = "Login attempt",
            resourceType = "user",
            resourceId = ldapInjection,
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0"
        )

        assertNotNull(event)
        assertTrue(event.userId.contains("*"))
    }

    @Test
    fun `should handle various encoding attacks`() {
        val encodedInputs = listOf(
            "%3Cscript%3Ealert('XSS')%3C/script%3E", // URL encoded
            "&#60;script&#62;alert('XSS')&#60;/script&#62;", // HTML entity encoded
            "\\x3cscript\\x3ealert('XSS')\\x3c/script\\x3e" // Hex encoded
        )

        encodedInputs.forEach { input ->
            val event = auditService.createEvent(
                eventType = EventType.RESOURCE_CREATED,
                userId = "user-123",
                action = input,
                resourceType = "document",
                resourceId = "doc-1",
                ipAddress = "192.168.1.1",
                userAgent = "Mozilla/5.0"
            )
            assertNotNull(event)
        }
    }

    @Test
    fun `should handle metadata with malicious content`() {
        val maliciousMetadata = mapOf(
            "sql" to "'; DROP TABLE users; --",
            "xss" to "<img src=x onerror=alert('XSS')>",
            "cmd" to "| cat /etc/passwd",
            "json" to """{"__proto__": {"admin": true}}"""
        )

        val event = auditService.createEvent(
            eventType = EventType.RESOURCE_CREATED,
            userId = "user-123",
            action = "Create with malicious metadata",
            resourceType = "document",
            resourceId = "doc-1",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0",
            metadata = maliciousMetadata
        )

        assertNotNull(event)
        val found = auditService.getEventById(event.id)
        assertNotNull(found)
        assertTrue(found.metadata.containsKey("sql"))
        assertTrue(found.metadata.containsKey("xss"))
    }
}
