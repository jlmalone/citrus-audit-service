package com.citrus.audit.routes

import com.citrus.audit.models.*
import com.citrus.audit.services.AuditService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant
import java.util.UUID

fun Route.auditRoutes(auditService: AuditService) {

    route("/audit") {

        /**
         * Log a new audit event
         */
        post("/events") {
            try {
                val params = call.receiveParameters()

                val event = AuditEvent(
                    eventType = EventType.valueOf(params["eventType"] ?: "RESOURCE_ACCESSED"),
                    userId = params["userId"],
                    username = params["username"],
                    ipAddress = params["ipAddress"] ?: call.request.origin.remoteHost,
                    userAgent = params["userAgent"] ?: call.request.headers["User-Agent"],
                    resourceType = params["resourceType"],
                    resourceId = params["resourceId"],
                    action = params["action"] ?: "UNKNOWN",
                    description = params["description"] ?: "",
                    severity = params["severity"]?.let { EventSeverity.valueOf(it) } ?: EventSeverity.INFO,
                    success = params["success"]?.toBoolean() ?: true,
                    errorMessage = params["errorMessage"]
                )

                val result = auditService.logEvent(event)
                call.respond(HttpStatusCode.Created, result)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            }
        }

        /**
         * Query audit events with filters
         */
        get("/events") {
            val filter = AuditQueryFilter(
                userId = call.request.queryParameters["userId"],
                eventType = call.request.queryParameters["eventType"],
                startTime = call.request.queryParameters["startTime"],
                endTime = call.request.queryParameters["endTime"],
                resourceType = call.request.queryParameters["resourceType"],
                resourceId = call.request.queryParameters["resourceId"],
                severity = call.request.queryParameters["severity"],
                successOnly = call.request.queryParameters["successOnly"]?.toBoolean(),
                limit = call.request.queryParameters["limit"]?.toInt() ?: 100,
                offset = call.request.queryParameters["offset"]?.toInt() ?: 0
            )

            val events = auditService.getEvents(filter)
            call.respond(HttpStatusCode.OK, events)
        }

        /**
         * Get specific event by ID
         */
        get("/events/{id}") {
            val id = call.parameters["id"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))

            val event = auditService.getEventById(id)
            if (event != null) {
                call.respond(HttpStatusCode.OK, event)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Event not found"))
            }
        }

        /**
         * Get user activity summary
         */
        get("/users/{userId}/activity") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User ID required"))

            val limit = call.request.queryParameters["limit"]?.toInt() ?: 10

            val summary = auditService.getUserActivitySummary(userId, limit)
            call.respond(HttpStatusCode.OK, summary)
        }

        /**
         * Get access logs
         */
        get("/access-logs") {
            val resourceType = call.request.queryParameters["resourceType"]
            val resourceId = call.request.queryParameters["resourceId"]
            val limit = call.request.queryParameters["limit"]?.toInt() ?: 100

            val logs = auditService.getAccessLogs(resourceType, resourceId, limit)
            call.respond(HttpStatusCode.OK, logs)
        }

        /**
         * Generate receipt for an event
         */
        get("/receipts/{eventId}") {
            val eventId = call.parameters["eventId"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid event ID"))

            val receipt = auditService.generateReceipt(eventId)
            if (receipt != null) {
                call.respond(HttpStatusCode.OK, receipt)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Event not found"))
            }
        }

        /**
         * Get receipts for a user
         */
        get("/users/{userId}/receipts") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User ID required"))

            val limit = call.request.queryParameters["limit"]?.toInt() ?: 50

            val receipts = auditService.getUserReceipts(userId, limit)
            call.respond(HttpStatusCode.OK, receipts)
        }

        /**
         * Health check
         */
        get("/health") {
            call.respond(
                HttpStatusCode.OK, mapOf(
                    "status" to "healthy",
                    "service" to "citrus-audit-service",
                    "timestamp" to Instant.now().toString()
                )
            )
        }
    }
}
