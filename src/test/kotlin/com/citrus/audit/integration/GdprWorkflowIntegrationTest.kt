package com.citrus.audit.integration

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.ActivityType
import com.citrus.audit.domain.DataRetentionPolicy
import com.citrus.audit.domain.DataType
import com.citrus.audit.domain.ErasureStatus
import com.citrus.audit.domain.UserActivity
import com.citrus.audit.repository.UserActivityRepository
import com.citrus.audit.service.DataRetentionService
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GdprWorkflowIntegrationTest {

    private val retentionService = DataRetentionService()
    private val activityRepository = UserActivityRepository()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should complete full GDPR erasure workflow`() {
        val userId = "gdpr-test-user-1"

        // Step 1: Create user activities
        val activities = (1..10).map { i ->
            UserActivity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                sessionId = "session-1",
                activityType = ActivityType.PAGE_VIEW,
                timestamp = Clock.System.now(),
                duration = 1000L,
                details = "Activity $i"
            )
        }

        activities.forEach { activityRepository.create(it) }

        // Verify activities were created
        val createdActivities = activityRepository.findByUserId(userId)
        assertEquals(10, createdActivities.size)

        // Step 2: Request GDPR erasure
        val erasureRequest = retentionService.requestGdprErasure(userId)
        assertNotNull(erasureRequest)
        assertEquals(userId, erasureRequest.userId)
        assertEquals(ErasureStatus.PENDING, erasureRequest.status)

        // Step 3: Process the erasure
        val processed = retentionService.processGdprErasure(erasureRequest.id)
        assertNotNull(processed)
        assertEquals(ErasureStatus.COMPLETED, processed.status)
        assertNotNull(processed.completedAt)
        assertTrue(processed.deletedRecords.containsKey("user_activities"))

        // Step 4: Verify data was erased
        val remainingActivities = activityRepository.findByUserId(userId)
        assertEquals(0, remainingActivities.size)

        // Step 5: Verify erasure request can be retrieved
        val retrievedRequest = retentionService.getErasureRequest(erasureRequest.id)
        assertNotNull(retrievedRequest)
        assertEquals(ErasureStatus.COMPLETED, retrievedRequest.status)
    }

    @Test
    fun `should handle data retention policy application`() {
        val now = Clock.System.now()

        // Create retention policies
        val auditPolicy = DataRetentionPolicy(
            id = "policy-audit",
            name = "Audit Events Retention",
            description = "Retain audit events for 90 days",
            dataType = DataType.AUDIT_EVENTS,
            retentionPeriodDays = 90,
            autoDelete = true,
            createdAt = now,
            updatedAt = now,
            isActive = true
        )

        val activityPolicy = DataRetentionPolicy(
            id = "policy-activity",
            name = "User Activities Retention",
            description = "Retain user activities for 60 days",
            dataType = DataType.USER_ACTIVITIES,
            retentionPeriodDays = 60,
            autoDelete = true,
            createdAt = now,
            updatedAt = now,
            isActive = true
        )

        retentionService.createRetentionPolicy(auditPolicy)
        retentionService.createRetentionPolicy(activityPolicy)

        // Verify policies were created
        val activePolicies = retentionService.getActivePolicies()
        assertEquals(2, activePolicies.size)

        // Apply retention policies
        val results = retentionService.applyRetentionPolicies()
        assertNotNull(results)
        assertTrue(results.containsKey(DataType.AUDIT_EVENTS))
        assertTrue(results.containsKey(DataType.USER_ACTIVITIES))
    }

    @Test
    fun `should track multiple erasure requests for different users`() {
        val users = listOf("user-gdpr-1", "user-gdpr-2", "user-gdpr-3")

        // Create erasure requests
        val requests = users.map { userId ->
            retentionService.requestGdprErasure(userId)
        }

        assertEquals(3, requests.size)

        // Verify all requests are pending
        val pending = retentionService.getPendingErasureRequests()
        assertTrue(pending.size >= 3)

        // Process one request
        retentionService.processGdprErasure(requests[0].id)

        // Verify one less pending
        val pendingAfter = retentionService.getPendingErasureRequests()
        assertEquals(pending.size - 1, pendingAfter.size)
    }

    @Test
    fun `should maintain erasure request history`() {
        val userId = "user-history-test"

        // Create multiple erasure requests over time
        val request1 = retentionService.requestGdprErasure(userId)
        retentionService.processGdprErasure(request1.id)

        // Create some new activities
        activityRepository.create(UserActivity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            sessionId = "session-2",
            activityType = ActivityType.BUTTON_CLICK,
            timestamp = Clock.System.now(),
            duration = 500L,
            details = "New activity"
        ))

        // Request another erasure
        val request2 = retentionService.requestGdprErasure(userId)

        // Verify history
        val history = retentionService.getUserErasureRequests(userId)
        assertEquals(2, history.size)

        val completedCount = history.count { it.status == ErasureStatus.COMPLETED }
        val pendingCount = history.count { it.status == ErasureStatus.PENDING }

        assertEquals(1, completedCount)
        assertEquals(1, pendingCount)
    }
}
