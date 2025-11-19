package com.citrus.audit.repository

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.*
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DataRetentionRepositoryTest {

    private val repository = DataRetentionRepository()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should create and find retention policy`() {
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

        repository.createPolicy(policy)
        val found = repository.findPolicyById("policy-1")

        assertNotNull(found)
        assertEquals(policy.id, found.id)
        assertEquals(policy.retentionPeriodDays, found.retentionPeriodDays)
    }

    @Test
    fun `should find active policies`() {
        val now = Clock.System.now()
        val policy1 = createTestPolicy("policy-1", DataType.AUDIT_EVENTS, true)
        val policy2 = createTestPolicy("policy-2", DataType.USER_ACTIVITIES, true)
        val policy3 = createTestPolicy("policy-3", DataType.ACCESS_LOGS, false)

        repository.createPolicy(policy1)
        repository.createPolicy(policy2)
        repository.createPolicy(policy3)

        val activePolicies = repository.findActivePolicies()
        assertEquals(2, activePolicies.size)
    }

    @Test
    fun `should find policy by data type`() {
        val policy1 = createTestPolicy("policy-1", DataType.AUDIT_EVENTS, true)
        val policy2 = createTestPolicy("policy-2", DataType.USER_ACTIVITIES, true)

        repository.createPolicy(policy1)
        repository.createPolicy(policy2)

        val found = repository.findPolicyByDataType(DataType.AUDIT_EVENTS)
        assertNotNull(found)
        assertEquals("policy-1", found.id)
    }

    @Test
    fun `should create and find GDPR erasure request`() {
        val now = Clock.System.now()
        val request = GdprErasureRequest(
            id = "request-1",
            userId = "user-123",
            requestedAt = now,
            status = ErasureStatus.PENDING
        )

        repository.createErasureRequest(request)
        val found = repository.findErasureRequestById("request-1")

        assertNotNull(found)
        assertEquals(request.id, found.id)
        assertEquals(request.userId, found.userId)
        assertEquals(ErasureStatus.PENDING, found.status)
    }

    @Test
    fun `should update erasure request`() {
        val now = Clock.System.now()
        val request = GdprErasureRequest(
            id = "request-1",
            userId = "user-123",
            requestedAt = now,
            status = ErasureStatus.PENDING
        )

        repository.createErasureRequest(request)

        val updated = request.copy(
            status = ErasureStatus.COMPLETED,
            completedAt = now,
            deletedRecords = mapOf("user_activities" to 10L)
        )
        repository.updateErasureRequest(updated)

        val found = repository.findErasureRequestById("request-1")
        assertNotNull(found)
        assertEquals(ErasureStatus.COMPLETED, found.status)
        assertNotNull(found.completedAt)
        assertEquals(10L, found.deletedRecords["user_activities"])
    }

    @Test
    fun `should find erasure requests by user id`() {
        val now = Clock.System.now()
        val request1 = createTestErasureRequest("request-1", "user-123")
        val request2 = createTestErasureRequest("request-2", "user-123")
        val request3 = createTestErasureRequest("request-3", "user-456")

        repository.createErasureRequest(request1)
        repository.createErasureRequest(request2)
        repository.createErasureRequest(request3)

        val userRequests = repository.findErasureRequestsByUserId("user-123")
        assertEquals(2, userRequests.size)
    }

    @Test
    fun `should find pending erasure requests`() {
        val request1 = createTestErasureRequest("request-1", "user-123")
            .copy(status = ErasureStatus.PENDING)
        val request2 = createTestErasureRequest("request-2", "user-456")
            .copy(status = ErasureStatus.COMPLETED)

        repository.createErasureRequest(request1)
        repository.createErasureRequest(request2)

        val pendingRequests = repository.findPendingErasureRequests()
        assertEquals(1, pendingRequests.size)
        assertEquals("request-1", pendingRequests[0].id)
    }

    private fun createTestPolicy(id: String, dataType: DataType, isActive: Boolean): DataRetentionPolicy {
        val now = Clock.System.now()
        return DataRetentionPolicy(
            id = id,
            name = "Test Policy",
            description = "Test Description",
            dataType = dataType,
            retentionPeriodDays = 90,
            autoDelete = true,
            createdAt = now,
            updatedAt = now,
            isActive = isActive
        )
    }

    private fun createTestErasureRequest(id: String, userId: String): GdprErasureRequest {
        return GdprErasureRequest(
            id = id,
            userId = userId,
            requestedAt = Clock.System.now(),
            status = ErasureStatus.PENDING
        )
    }
}
