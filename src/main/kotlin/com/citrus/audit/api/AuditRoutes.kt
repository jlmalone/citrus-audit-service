package com.citrus.audit.api

import com.citrus.audit.domain.AuditEvent
import com.citrus.audit.domain.EventType
import com.citrus.audit.service.AuditService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CreateAuditEventRequest(
    val eventType: EventType,
    val userId: String,
    val action: String,
    val resourceType: String,
    val resourceId: String,
    val ipAddress: String,
    val userAgent: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class TimeRangeRequest(
    val start: Instant,
    val end: Instant
)

fun Route.auditRoutes(auditService: AuditService = AuditService()) {

    route("/api/audit") {

        post("/events") {
            val request = call.receive<CreateAuditEventRequest>()
            val event = auditService.createEvent(
                eventType = request.eventType,
                userId = request.userId,
                action = request.action,
                resourceType = request.resourceType,
                resourceId = request.resourceId,
                ipAddress = request.ipAddress,
                userAgent = request.userAgent,
                metadata = request.metadata
            )
            call.respond(HttpStatusCode.Created, event)
        }

        get("/events/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing event ID")
            )
            val event = auditService.getEventById(id)
            if (event != null) {
                call.respond(event)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Event not found"))
            }
        }

        get("/events/user/{userId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing user ID")
            )
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
            val events = auditService.getUserEvents(userId, limit)
            call.respond(events)
        }

        post("/events/range") {
            val request = call.receive<TimeRangeRequest>()
            val events = auditService.getEventsInTimeRange(request.start, request.end)
            call.respond(events)
        }

        get("/events/type/{eventType}") {
            val eventTypeStr = call.parameters["eventType"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing event type")
            )
            try {
                val eventType = EventType.valueOf(eventTypeStr)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                val events = auditService.getEventsByType(eventType, limit)
                call.respond(events)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid event type"))
            }
        }

        get("/events/user/{userId}/count") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing user ID")
            )
            val count = auditService.getUserEventCount(userId)
            call.respond(mapOf("userId" to userId, "count" to count))
        }
    }
}
