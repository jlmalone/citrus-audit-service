package com.citrus.audit.repository

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.ActivityType
import com.citrus.audit.domain.UserActivity
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class UserActivityRepositoryTest {

    private val repository = UserActivityRepository()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should create and find user activity by id`() {
        val activity = createTestActivity("activity-1", "user-123", "session-1")

        repository.create(activity)
        val found = repository.findById("activity-1")

        assertNotNull(found)
        assertEquals(activity.id, found.id)
        assertEquals(activity.userId, found.userId)
        assertEquals(activity.sessionId, found.sessionId)
    }

    @Test
    fun `should find activities by user id`() {
        val activity1 = createTestActivity("activity-1", "user-123", "session-1")
        val activity2 = createTestActivity("activity-2", "user-123", "session-2")
        val activity3 = createTestActivity("activity-3", "user-456", "session-3")

        repository.create(activity1)
        repository.create(activity2)
        repository.create(activity3)

        val userActivities = repository.findByUserId("user-123")
        assertEquals(2, userActivities.size)
    }

    @Test
    fun `should find activities by session id`() {
        val activity1 = createTestActivity("activity-1", "user-123", "session-1")
        val activity2 = createTestActivity("activity-2", "user-456", "session-1")

        repository.create(activity1)
        repository.create(activity2)

        val sessionActivities = repository.findBySessionId("session-1")
        assertEquals(2, sessionActivities.size)
    }

    @Test
    fun `should find activities by activity type`() {
        val activity1 = createTestActivity("activity-1", "user-123", "session-1")
            .copy(activityType = ActivityType.PAGE_VIEW)
        val activity2 = createTestActivity("activity-2", "user-456", "session-2")
            .copy(activityType = ActivityType.BUTTON_CLICK)

        repository.create(activity1)
        repository.create(activity2)

        val pageViews = repository.findByActivityType(ActivityType.PAGE_VIEW)
        assertEquals(1, pageViews.size)
        assertEquals("activity-1", pageViews[0].id)
    }

    @Test
    fun `should delete activities by user id`() {
        val activity1 = createTestActivity("activity-1", "user-123", "session-1")
        val activity2 = createTestActivity("activity-2", "user-123", "session-2")
        val activity3 = createTestActivity("activity-3", "user-456", "session-3")

        repository.create(activity1)
        repository.create(activity2)
        repository.create(activity3)

        val deleted = repository.deleteByUserId("user-123")
        assertEquals(2, deleted)

        assertNull(repository.findById("activity-1"))
        assertNull(repository.findById("activity-2"))
        assertNotNull(repository.findById("activity-3"))
    }

    @Test
    fun `should delete old activities`() {
        val now = Clock.System.now()
        val old = now.minus(100.days)

        val oldActivity = createTestActivity("activity-old", "user-123", "session-1")
            .copy(timestamp = old)
        val newActivity = createTestActivity("activity-new", "user-123", "session-2")
            .copy(timestamp = now)

        repository.create(oldActivity)
        repository.create(newActivity)

        val deleted = repository.deleteOlderThan(now.minus(50.days))
        assertEquals(1, deleted)

        assertNull(repository.findById("activity-old"))
        assertNotNull(repository.findById("activity-new"))
    }

    private fun createTestActivity(id: String, userId: String, sessionId: String): UserActivity {
        return UserActivity(
            id = id,
            userId = userId,
            sessionId = sessionId,
            activityType = ActivityType.PAGE_VIEW,
            timestamp = Clock.System.now(),
            duration = 1000L,
            details = "Test activity"
        )
    }
}
