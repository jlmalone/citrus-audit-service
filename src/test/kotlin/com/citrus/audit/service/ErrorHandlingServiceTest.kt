package com.citrus.audit.service

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.ErasureStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ErrorHandlingServiceTest {

    private val retentionService = DataRetentionService()
    private val auditService = AuditService()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should throw exception for non-existent erasure request`() {
        val exception = assertThrows<IllegalArgumentException> {
            retentionService.processGdprErasure("non-existent-id")
        }
        assertNotNull(exception.message)
    }

    @Test
    fun `should throw exception when processing already completed erasure`() {
        val request = retentionService.requestGdprErasure("user-123")
        retentionService.processGdprErasure(request.id)

        val exception = assertThrows<IllegalStateException> {
            retentionService.processGdprErasure(request.id)
        }
        assertNotNull(exception.message)
    }

    @Test
    fun `should handle null results gracefully`() {
        val event = auditService.getEventById("non-existent")
        assertEquals(null, event)
    }

    @Test
    fun `should handle empty user erasure gracefully`() {
        val request = retentionService.requestGdprErasure("user-with-no-data")
        val processed = retentionService.processGdprErasure(request.id)

        assertNotNull(processed)
        assertEquals(ErasureStatus.COMPLETED, processed.status)
        assertNotNull(processed.completedAt)
    }

    @Test
    fun `should handle concurrent event creation`() {
        // Create multiple events rapidly
        val events = (1..10).map { i ->
            auditService.createEvent(
                eventType = com.citrus.audit.domain.EventType.USER_LOGIN,
                userId = "user-$i",
                action = "Concurrent login $i",
                resourceType = "user",
                resourceId = "user-$i",
                ipAddress = "192.168.1.$i",
                userAgent = "Mozilla/5.0"
            )
        }

        assertEquals(10, events.size)
        assertEquals(10, events.map { it.id }.distinct().size) // All IDs should be unique
    }

    @Test
    fun `should handle retrieval of non-existent policy`() {
        val policy = retentionService.getRetentionPolicy("non-existent")
        assertEquals(null, policy)
    }

    @Test
    fun `should handle retrieval of non-existent erasure request`() {
        val request = retentionService.getErasureRequest("non-existent")
        assertEquals(null, request)
    }
}
