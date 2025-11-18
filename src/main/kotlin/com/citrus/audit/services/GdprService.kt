package com.citrus.audit.services

import com.citrus.audit.database.AuditEvents
import com.citrus.audit.database.DatabaseFactory.dbQuery
import com.citrus.audit.database.GdprErasureRequests
import com.citrus.audit.models.*
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.util.UUID

class GdprService(private val auditService: AuditService) {

    /**
     * Request GDPR erasure for a user
     */
    suspend fun requestErasure(request: GdprErasureRequest): GdprErasureResponse = dbQuery {
        val requestId = UUID.randomUUID()
        val now = Instant.now()

        // Create erasure request record
        GdprErasureRequests.insert {
            it[id] = requestId
            it[userId] = request.userId
            it[requestedBy] = request.requestedBy
            it[reason] = request.reason
            it[status] = "PENDING"
            it[requestedAt] = now
        }

        // Log the erasure request
        auditService.logEvent(
            AuditEvent(
                eventType = EventType.GDPR_ERASURE_REQUESTED,
                userId = request.userId,
                username = null,
                ipAddress = null,
                userAgent = null,
                resourceType = "user_data",
                resourceId = request.userId,
                action = "GDPR_ERASURE_REQUEST",
                description = "GDPR erasure requested by ${request.requestedBy}. Reason: ${request.reason}",
                severity = EventSeverity.INFO,
                metadata = mapOf(
                    "requestedBy" to request.requestedBy,
                    "reason" to request.reason,
                    "requestId" to requestId.toString()
                )
            )
        )

        GdprErasureResponse(
            requestId = requestId.toString(),
            userId = request.userId,
            eventsErased = 0,
            timestamp = now.toString(),
            status = "PENDING"
        )
    }

    /**
     * Execute GDPR erasure - anonymize user data
     */
    suspend fun executeErasure(requestId: UUID): GdprErasureResponse = dbQuery {
        // Get the erasure request
        val request = GdprErasureRequests.selectAll()
            .where { GdprErasureRequests.id eq requestId }
            .singleOrNull() ?: throw IllegalArgumentException("Erasure request not found")

        val userId = request[GdprErasureRequests.userId]

        // Anonymize user data in audit events
        // We keep the events for audit trail but remove PII
        val eventsUpdated = AuditEvents.update({ AuditEvents.userId eq userId }) {
            it[AuditEvents.userId] = "ANONYMIZED_${requestId}"
            it[username] = "ANONYMIZED"
            it[ipAddress] = null
            it[userAgent] = null
        }

        // Update erasure request status
        val now = Instant.now()
        GdprErasureRequests.update({ GdprErasureRequests.id eq requestId }) {
            it[status] = "COMPLETED"
            it[eventsErased] = eventsUpdated
            it[completedAt] = now
        }

        // Log the completion
        auditService.logEvent(
            AuditEvent(
                eventType = EventType.GDPR_ERASURE_COMPLETED,
                userId = "ANONYMIZED_${requestId}",
                username = "ANONYMIZED",
                ipAddress = null,
                userAgent = null,
                resourceType = "user_data",
                resourceId = userId,
                action = "GDPR_ERASURE_COMPLETE",
                description = "GDPR erasure completed for user. $eventsUpdated events anonymized.",
                severity = EventSeverity.INFO,
                metadata = mapOf(
                    "requestId" to requestId.toString(),
                    "eventsAnonymized" to eventsUpdated.toString()
                )
            )
        )

        GdprErasureResponse(
            requestId = requestId.toString(),
            userId = userId,
            eventsErased = eventsUpdated,
            timestamp = now.toString(),
            status = "COMPLETED"
        )
    }

    /**
     * Get erasure request status
     */
    suspend fun getErasureStatus(requestId: UUID): GdprErasureResponse? = dbQuery {
        GdprErasureRequests.selectAll()
            .where { GdprErasureRequests.id eq requestId }
            .map { row ->
                GdprErasureResponse(
                    requestId = row[GdprErasureRequests.id].toString(),
                    userId = row[GdprErasureRequests.userId],
                    eventsErased = row[GdprErasureRequests.eventsErased] ?: 0,
                    timestamp = (row[GdprErasureRequests.completedAt]
                        ?: row[GdprErasureRequests.requestedAt]).toString(),
                    status = row[GdprErasureRequests.status]
                )
            }
            .singleOrNull()
    }

    /**
     * Get all erasure requests
     */
    suspend fun getAllErasureRequests(status: String? = null): List<GdprErasureResponse> = dbQuery {
        var query = GdprErasureRequests.selectAll()

        status?.let {
            query = query.where { GdprErasureRequests.status eq it }
        }

        query.map { row ->
            GdprErasureResponse(
                requestId = row[GdprErasureRequests.id].toString(),
                userId = row[GdprErasureRequests.userId],
                eventsErased = row[GdprErasureRequests.eventsErased] ?: 0,
                timestamp = (row[GdprErasureRequests.completedAt]
                    ?: row[GdprErasureRequests.requestedAt]).toString(),
                status = row[GdprErasureRequests.status]
            )
        }
    }

    /**
     * Get erasure requests for a specific user
     */
    suspend fun getUserErasureRequests(userId: String): List<GdprErasureResponse> = dbQuery {
        GdprErasureRequests.selectAll()
            .where { GdprErasureRequests.userId eq userId }
            .orderBy(GdprErasureRequests.requestedAt, SortOrder.DESC)
            .map { row ->
                GdprErasureResponse(
                    requestId = row[GdprErasureRequests.id].toString(),
                    userId = row[GdprErasureRequests.userId],
                    eventsErased = row[GdprErasureRequests.eventsErased] ?: 0,
                    timestamp = (row[GdprErasureRequests.completedAt]
                        ?: row[GdprErasureRequests.requestedAt]).toString(),
                    status = row[GdprErasureRequests.status]
                )
            }
    }

    /**
     * Export user data for GDPR data portability
     */
    suspend fun exportUserData(userId: String): Map<String, Any> = dbQuery {
        val events = AuditEvents.selectAll()
            .where { AuditEvents.userId eq userId }
            .orderBy(AuditEvents.timestamp, SortOrder.DESC)
            .toList()

        val erasureRequests = GdprErasureRequests.selectAll()
            .where { GdprErasureRequests.userId eq userId }
            .toList()

        mapOf(
            "userId" to userId,
            "exportDate" to Instant.now().toString(),
            "totalEvents" to events.size,
            "events" to events.map { row ->
                mapOf(
                    "id" to row[AuditEvents.id].toString(),
                    "eventType" to row[AuditEvents.eventType],
                    "timestamp" to row[AuditEvents.timestamp].toString(),
                    "action" to row[AuditEvents.action],
                    "description" to row[AuditEvents.description],
                    "resourceType" to row[AuditEvents.resourceType],
                    "resourceId" to row[AuditEvents.resourceId],
                    "success" to row[AuditEvents.success]
                )
            },
            "erasureRequests" to erasureRequests.map { row ->
                mapOf(
                    "requestId" to row[GdprErasureRequests.id].toString(),
                    "requestedAt" to row[GdprErasureRequests.requestedAt].toString(),
                    "status" to row[GdprErasureRequests.status],
                    "reason" to row[GdprErasureRequests.reason]
                )
            }
        )
    }
}
