package com.citrus.audit.services

import com.citrus.audit.database.AuditEvents
import com.citrus.audit.database.DatabaseFactory.dbQuery
import com.citrus.audit.models.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

class AuditService {

    /**
     * Log an audit event
     */
    suspend fun logEvent(event: AuditEvent): AuditEventDto = dbQuery {
        AuditEvents.insert {
            it[id] = event.id
            it[eventType] = event.eventType.name
            it[timestamp] = event.timestamp
            it[userId] = event.userId
            it[username] = event.username
            it[ipAddress] = event.ipAddress
            it[userAgent] = event.userAgent
            it[resourceType] = event.resourceType
            it[resourceId] = event.resourceId
            it[action] = event.action
            it[description] = event.description
            it[severity] = event.severity.name
            it[metadata] = Json.encodeToString(event.metadata)
            it[success] = event.success
            it[errorMessage] = event.errorMessage
            it[createdAt] = Instant.now()
        }

        event.toDto()
    }

    /**
     * Get audit events with filtering
     */
    suspend fun getEvents(filter: AuditQueryFilter): List<AuditEventDto> = dbQuery {
        var query = AuditEvents.selectAll()

        // Apply filters
        filter.userId?.let { query = query.where { AuditEvents.userId eq it } }
        filter.eventType?.let { query = query.where { AuditEvents.eventType eq it } }
        filter.resourceType?.let { query = query.where { AuditEvents.resourceType eq it } }
        filter.resourceId?.let { query = query.where { AuditEvents.resourceId eq it } }
        filter.severity?.let { query = query.where { AuditEvents.severity eq it } }
        filter.successOnly?.let { if (it) query = query.where { AuditEvents.success eq true } }

        filter.startTime?.let {
            val start = Instant.parse(it)
            query = query.where { AuditEvents.timestamp greater start }
        }
        filter.endTime?.let {
            val end = Instant.parse(it)
            query = query.where { AuditEvents.timestamp less end }
        }

        query
            .orderBy(AuditEvents.timestamp, SortOrder.DESC)
            .limit(filter.limit, filter.offset.toLong())
            .map { rowToAuditEvent(it) }
    }

    /**
     * Get event by ID
     */
    suspend fun getEventById(id: UUID): AuditEventDto? = dbQuery {
        AuditEvents.selectAll()
            .where { AuditEvents.id eq id }
            .map { rowToAuditEvent(it) }
            .singleOrNull()
    }

    /**
     * Get user activity summary
     */
    suspend fun getUserActivitySummary(userId: String, limit: Int = 10): UserActivitySummary = dbQuery {
        val events = AuditEvents.selectAll()
            .where { AuditEvents.userId eq userId }
            .orderBy(AuditEvents.timestamp, SortOrder.DESC)
            .toList()

        val loginCount = events.count { it[AuditEvents.eventType] == EventType.USER_LOGIN.name }
        val lastLogin = events
            .firstOrNull { it[AuditEvents.eventType] == EventType.USER_LOGIN.name }
            ?.get(AuditEvents.timestamp)
            ?.toString()

        val resourceAccessCount = events.count {
            it[AuditEvents.eventType] == EventType.RESOURCE_ACCESSED.name
        }
        val failedActionCount = events.count { !it[AuditEvents.success] }

        val recentActions = events.take(limit).map { rowToAuditEvent(it) }

        UserActivitySummary(
            userId = userId,
            username = events.firstOrNull()?.get(AuditEvents.username),
            loginCount = loginCount,
            lastLogin = lastLogin,
            resourceAccessCount = resourceAccessCount,
            failedActionCount = failedActionCount,
            mostRecentActions = recentActions
        )
    }

    /**
     * Get access logs
     */
    suspend fun getAccessLogs(
        resourceType: String? = null,
        resourceId: String? = null,
        limit: Int = 100
    ): List<AccessLogEntry> = dbQuery {
        var query = AuditEvents.selectAll()
            .where { AuditEvents.eventType eq EventType.RESOURCE_ACCESSED.name }

        resourceType?.let { query = query.andWhere { AuditEvents.resourceType eq it } }
        resourceId?.let { query = query.andWhere { AuditEvents.resourceId eq it } }

        query
            .orderBy(AuditEvents.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { row ->
                AccessLogEntry(
                    id = row[AuditEvents.id].toString(),
                    timestamp = row[AuditEvents.timestamp].toString(),
                    userId = row[AuditEvents.userId],
                    username = row[AuditEvents.username],
                    ipAddress = row[AuditEvents.ipAddress],
                    resourceType = row[AuditEvents.resourceType],
                    resourceId = row[AuditEvents.resourceId],
                    action = row[AuditEvents.action],
                    success = row[AuditEvents.success]
                )
            }
    }

    /**
     * Generate audit receipt
     */
    suspend fun generateReceipt(eventId: UUID): AuditReceipt? = dbQuery {
        val event = AuditEvents.selectAll()
            .where { AuditEvents.id eq eventId }
            .singleOrNull() ?: return@dbQuery null

        val checksum = generateChecksum(
            eventId.toString(),
            event[AuditEvents.eventType],
            event[AuditEvents.timestamp].toString(),
            event[AuditEvents.userId] ?: "anonymous"
        )

        AuditReceipt(
            receiptId = UUID.randomUUID().toString(),
            eventId = eventId.toString(),
            timestamp = event[AuditEvents.timestamp].toString(),
            eventType = event[AuditEvents.eventType],
            userId = event[AuditEvents.userId],
            action = event[AuditEvents.action],
            checksum = checksum
        )
    }

    /**
     * Get all receipts for a user
     */
    suspend fun getUserReceipts(userId: String, limit: Int = 50): List<AuditReceipt> = dbQuery {
        AuditEvents.selectAll()
            .where { AuditEvents.userId eq userId }
            .orderBy(AuditEvents.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { row ->
                val eventId = row[AuditEvents.id]
                val checksum = generateChecksum(
                    eventId.toString(),
                    row[AuditEvents.eventType],
                    row[AuditEvents.timestamp].toString(),
                    userId
                )

                AuditReceipt(
                    receiptId = UUID.randomUUID().toString(),
                    eventId = eventId.toString(),
                    timestamp = row[AuditEvents.timestamp].toString(),
                    eventType = row[AuditEvents.eventType],
                    userId = userId,
                    action = row[AuditEvents.action],
                    checksum = checksum
                )
            }
    }

    /**
     * Helper to convert ResultRow to AuditEventDto
     */
    private fun rowToAuditEvent(row: ResultRow): AuditEventDto {
        return AuditEventDto(
            id = row[AuditEvents.id].toString(),
            eventType = row[AuditEvents.eventType],
            timestamp = row[AuditEvents.timestamp].toString(),
            userId = row[AuditEvents.userId],
            username = row[AuditEvents.username],
            ipAddress = row[AuditEvents.ipAddress],
            userAgent = row[AuditEvents.userAgent],
            resourceType = row[AuditEvents.resourceType],
            resourceId = row[AuditEvents.resourceId],
            action = row[AuditEvents.action],
            description = row[AuditEvents.description],
            severity = row[AuditEvents.severity],
            metadata = Json.decodeFromString(row[AuditEvents.metadata]),
            success = row[AuditEvents.success],
            errorMessage = row[AuditEvents.errorMessage]
        )
    }

    /**
     * Generate SHA-256 checksum for receipt
     */
    private fun generateChecksum(vararg parts: String): String {
        val data = parts.joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Extension function to convert AuditEvent to DTO
     */
    private fun AuditEvent.toDto() = AuditEventDto(
        id = id.toString(),
        eventType = eventType.name,
        timestamp = timestamp.toString(),
        userId = userId,
        username = username,
        ipAddress = ipAddress,
        userAgent = userAgent,
        resourceType = resourceType,
        resourceId = resourceId,
        action = action,
        description = description,
        severity = severity.name,
        metadata = metadata,
        success = success,
        errorMessage = errorMessage
    )
}
