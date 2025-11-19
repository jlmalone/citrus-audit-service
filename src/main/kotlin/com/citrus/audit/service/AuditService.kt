package com.citrus.audit.service

import com.citrus.audit.domain.AuditEvent
import com.citrus.audit.domain.EventType
import com.citrus.audit.repository.AuditEventRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID

class AuditService(private val repository: AuditEventRepository = AuditEventRepository()) {

    fun logEvent(event: AuditEvent): AuditEvent {
        return repository.create(event)
    }

    fun createEvent(
        eventType: EventType,
        userId: String,
        action: String,
        resourceType: String,
        resourceId: String,
        ipAddress: String,
        userAgent: String,
        metadata: Map<String, String> = emptyMap()
    ): AuditEvent {
        val event = AuditEvent(
            id = UUID.randomUUID().toString(),
            timestamp = Clock.System.now(),
            eventType = eventType,
            userId = userId,
            action = action,
            resourceType = resourceType,
            resourceId = resourceId,
            ipAddress = ipAddress,
            userAgent = userAgent,
            metadata = metadata
        )
        return repository.create(event)
    }

    fun getEventById(id: String): AuditEvent? {
        return repository.findById(id)
    }

    fun getUserEvents(userId: String, limit: Int = 100): List<AuditEvent> {
        return repository.findByUserId(userId, limit)
    }

    fun getEventsInTimeRange(start: Instant, end: Instant): List<AuditEvent> {
        return repository.findByTimeRange(start, end)
    }

    fun getEventsByType(eventType: EventType, limit: Int = 100): List<AuditEvent> {
        return repository.findByEventType(eventType, limit)
    }

    fun getUserEventCount(userId: String): Long {
        return repository.countByUserId(userId)
    }

    fun deleteOldEvents(cutoffDate: Instant): Int {
        return repository.deleteOlderThan(cutoffDate)
    }
}
