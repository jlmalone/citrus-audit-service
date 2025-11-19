package com.citrus.audit.repository

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.AuditEvent
import com.citrus.audit.domain.EventStatus
import com.citrus.audit.domain.EventType
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class AuditEventRepositoryTest {

    private val repository = AuditEventRepository()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should create and find audit event by id`() {
        val event = AuditEvent(
            id = "event-1",
            timestamp = Clock.System.now(),
            eventType = EventType.USER_LOGIN,
            userId = "user-123",
            action = "login",
            resourceType = "user",
            resourceId = "user-123",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0",
            metadata = mapOf("browser" to "Chrome"),
            status = EventStatus.SUCCESS
        )

        repository.create(event)
        val found = repository.findById("event-1")

        assertNotNull(found)
        assertEquals(event.id, found.id)
        assertEquals(event.userId, found.userId)
        assertEquals(event.eventType, found.eventType)
        assertEquals(event.action, found.action)
    }

    @Test
    fun `should find events by user id`() {
        val event1 = createTestEvent("event-1", "user-123")
        val event2 = createTestEvent("event-2", "user-123")
        val event3 = createTestEvent("event-3", "user-456")

        repository.create(event1)
        repository.create(event2)
        repository.create(event3)

        val userEvents = repository.findByUserId("user-123")
        assertEquals(2, userEvents.size)
        assertEquals(setOf("event-1", "event-2"), userEvents.map { it.id }.toSet())
    }

    @Test
    fun `should find events by time range`() {
        val now = Clock.System.now()
        val past = now.minus(2.days)
        val future = now.plus(2.days)

        val event1 = createTestEvent("event-1", "user-123").copy(timestamp = now)
        val event2 = createTestEvent("event-2", "user-123").copy(timestamp = past)

        repository.create(event1)
        repository.create(event2)

        val events = repository.findByTimeRange(past.minus(1.days), future)
        assertEquals(2, events.size)
    }

    @Test
    fun `should find events by event type`() {
        val event1 = createTestEvent("event-1", "user-123").copy(eventType = EventType.USER_LOGIN)
        val event2 = createTestEvent("event-2", "user-456").copy(eventType = EventType.USER_LOGIN)
        val event3 = createTestEvent("event-3", "user-789").copy(eventType = EventType.USER_LOGOUT)

        repository.create(event1)
        repository.create(event2)
        repository.create(event3)

        val loginEvents = repository.findByEventType(EventType.USER_LOGIN)
        assertEquals(2, loginEvents.size)
    }

    @Test
    fun `should count events by user id`() {
        val event1 = createTestEvent("event-1", "user-123")
        val event2 = createTestEvent("event-2", "user-123")

        repository.create(event1)
        repository.create(event2)

        val count = repository.countByUserId("user-123")
        assertEquals(2, count)
    }

    @Test
    fun `should delete old events`() {
        val now = Clock.System.now()
        val old = now.minus(100.days)

        val oldEvent = createTestEvent("event-old", "user-123").copy(timestamp = old)
        val newEvent = createTestEvent("event-new", "user-123").copy(timestamp = now)

        repository.create(oldEvent)
        repository.create(newEvent)

        val deleted = repository.deleteOlderThan(now.minus(50.days))
        assertEquals(1, deleted)

        assertNull(repository.findById("event-old"))
        assertNotNull(repository.findById("event-new"))
    }

    @Test
    fun `should return null for non-existent event`() {
        val found = repository.findById("non-existent")
        assertNull(found)
    }

    private fun createTestEvent(id: String, userId: String): AuditEvent {
        return AuditEvent(
            id = id,
            timestamp = Clock.System.now(),
            eventType = EventType.USER_LOGIN,
            userId = userId,
            action = "test action",
            resourceType = "test resource",
            resourceId = "resource-1",
            ipAddress = "192.168.1.1",
            userAgent = "Test Agent",
            metadata = emptyMap(),
            status = EventStatus.SUCCESS
        )
    }
}
