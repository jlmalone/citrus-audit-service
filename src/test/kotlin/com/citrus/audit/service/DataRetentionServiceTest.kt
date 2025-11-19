package com.citrus.audit.service

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.DataRetentionPolicy
import com.citrus.audit.domain.DataType
import com.citrus.audit.domain.ErasureStatus
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DataRetentionServiceTest {

    private val service = DataRetentionService()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should create retention policy`() {
        val now = Clock.System.now()
        val policy = DataRetentionPolicy(
            id = "policy-1",
            name = "Audit Event Retention",
            description = "Retain audit events for 90 days",
            dataType = DataType.AUDIT_EVENTS,
            retentionPeriodDays = 90,
            autoDelete = true,
            createdAt = now,
            updatedAt = now,
            isActive = true
        )

        val created = service.createRetentionPolicy(policy)
        assertNotNull(created)
        assertEquals("policy-1", created.id)
        assertEquals(90, created.retentionPeriodDays)
    }

    @Test
    fun `should get retention policy`() {
        val now = Clock.System.now()
        val policy = DataRetentionPolicy(
            id = "policy-1",
            name = "Test Policy",
            description = "Test",
            dataType = DataType.AUDIT_EVENTS,
            retentionPeriodDays = 90,
            autoDelete = true,
            createdAt = now,
            updatedAt = now,
            isActive = true
        )

        service.createRetentionPolicy(policy)
        val found = service.getRetentionPolicy("policy-1")

        assertNotNull(found)
        assertEquals("policy-1", found.id)
    }

    @Test
    fun `should get active policies`() {
        val now = Clock.System.now()
        val policy1 = DataRetentionPolicy(
            id = "policy-1",
            name = "Active Policy",
            description = "Test",
            dataType = DataType.AUDIT_EVENTS,
            retentionPeriodDays = 90,
            autoDelete = true,
            createdAt = now,
            updatedAt = now,
            isActive = true
        )

        val policy2 = DataRetentionPolicy(
            id = "policy-2",
            name = "Inactive Policy",
            description = "Test",
            dataType = DataType.USER_ACTIVITIES,
            retentionPeriodDays = 90,
            autoDelete = true,
            createdAt = now,
            updatedAt = now,
            isActive = false
        )

        service.createRetentionPolicy(policy1)
        service.createRetentionPolicy(policy2)

        val activePolicies = service.getActivePolicies()
        assertEquals(1, activePolicies.size)
        assertEquals("policy-1", activePolicies[0].id)
    }

    @Test
    fun `should apply retention policies`() {
        val now = Clock.System.now()
        val policy = DataRetentionPolicy(
            id = "policy-1",
            name = "Test Policy",
            description = "Test",
            dataType = DataType.AUDIT_EVENTS,
            retentionPeriodDays = 90,
            autoDelete = true,
            createdAt = now,
            updatedAt = now,
            isActive = true
        )

        service.createRetentionPolicy(policy)
        val results = service.applyRetentionPolicies()

        assertNotNull(results)
        assertTrue(results.containsKey(DataType.AUDIT_EVENTS))
    }

    @Test
    fun `should request GDPR erasure`() {
        val request = service.requestGdprErasure("user-123")

        assertNotNull(request.id)
        assertEquals("user-123", request.userId)
        assertEquals(ErasureStatus.PENDING, request.status)
    }

    @Test
    fun `should process GDPR erasure`() {
        // First create the request
        val request = service.requestGdprErasure("user-123")

        // Process it
        val processed = service.processGdprErasure(request.id)

        assertNotNull(processed)
        assertEquals(ErasureStatus.COMPLETED, processed.status)
        assertNotNull(processed.completedAt)
        assertNotNull(processed.deletedRecords)
    }

    @Test
    fun `should get erasure request`() {
        val request = service.requestGdprErasure("user-123")
        val found = service.getErasureRequest(request.id)

        assertNotNull(found)
        assertEquals(request.id, found.id)
        assertEquals("user-123", found.userId)
    }

    @Test
    fun `should get user erasure requests`() {
        service.requestGdprErasure("user-123")
        service.requestGdprErasure("user-123")
        service.requestGdprErasure("user-456")

        val requests = service.getUserErasureRequests("user-123")
        assertEquals(2, requests.size)
    }

    @Test
    fun `should get pending erasure requests`() {
        val request1 = service.requestGdprErasure("user-123")
        val request2 = service.requestGdprErasure("user-456")

        // Process one
        service.processGdprErasure(request2.id)

        val pendingRequests = service.getPendingErasureRequests()
        assertEquals(1, pendingRequests.size)
        assertEquals(request1.id, pendingRequests[0].id)
    }
}
