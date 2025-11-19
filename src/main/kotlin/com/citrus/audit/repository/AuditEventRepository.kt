package com.citrus.audit.repository

import com.citrus.audit.domain.AuditEvent
import com.citrus.audit.domain.EventStatus
import com.citrus.audit.domain.EventType
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class AuditEventRepository {

    fun create(event: AuditEvent): AuditEvent = transaction {
        AuditEvents.insert {
            it[id] = event.id
            it[timestamp] = event.timestamp.toJavaInstant()
            it[eventType] = event.eventType.name
            it[userId] = event.userId
            it[action] = event.action
            it[resourceType] = event.resourceType
            it[resourceId] = event.resourceId
            it[ipAddress] = event.ipAddress
            it[userAgent] = event.userAgent
            it[metadata] = Json.encodeToString(event.metadata)
            it[status] = event.status.name
        }
        event
    }

    fun findById(id: String): AuditEvent? = transaction {
        AuditEvents.select { AuditEvents.id eq id }
            .mapNotNull { toAuditEvent(it) }
            .singleOrNull()
    }

    fun findByUserId(userId: String, limit: Int = 100): List<AuditEvent> = transaction {
        AuditEvents.select { AuditEvents.userId eq userId }
            .orderBy(AuditEvents.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { toAuditEvent(it) }
    }

    fun findByTimeRange(start: Instant, end: Instant): List<AuditEvent> = transaction {
        AuditEvents.select {
            (AuditEvents.timestamp greaterEq start.toJavaInstant()) and
            (AuditEvents.timestamp lessEq end.toJavaInstant())
        }
        .orderBy(AuditEvents.timestamp, SortOrder.DESC)
        .map { toAuditEvent(it) }
    }

    fun findByEventType(eventType: EventType, limit: Int = 100): List<AuditEvent> = transaction {
        AuditEvents.select { AuditEvents.eventType eq eventType.name }
            .orderBy(AuditEvents.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { toAuditEvent(it) }
    }

    fun countByUserId(userId: String): Long = transaction {
        AuditEvents.select { AuditEvents.userId eq userId }.count()
    }

    fun deleteOlderThan(cutoffDate: Instant): Int = transaction {
        AuditEvents.deleteWhere { timestamp less cutoffDate.toJavaInstant() }
    }

    private fun toAuditEvent(row: ResultRow): AuditEvent {
        return AuditEvent(
            id = row[AuditEvents.id],
            timestamp = row[AuditEvents.timestamp].toKotlinInstant(),
            eventType = EventType.valueOf(row[AuditEvents.eventType]),
            userId = row[AuditEvents.userId],
            action = row[AuditEvents.action],
            resourceType = row[AuditEvents.resourceType],
            resourceId = row[AuditEvents.resourceId],
            ipAddress = row[AuditEvents.ipAddress],
            userAgent = row[AuditEvents.userAgent],
            metadata = Json.decodeFromString(row[AuditEvents.metadata]),
            status = EventStatus.valueOf(row[AuditEvents.status])
        )
    }
}
