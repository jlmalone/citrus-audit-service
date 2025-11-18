package com.citrus.audit.routes

import com.citrus.audit.models.EventType
import com.citrus.audit.services.RetentionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.retentionRoutes(retentionService: RetentionService) {

    route("/retention") {

        /**
         * Get all retention policies
         */
        get("/policies") {
            val policies = retentionService.getAllPolicies()
            call.respond(HttpStatusCode.OK, policies)
        }

        /**
         * Get retention policy for specific event type
         */
        get("/policies/{eventType}") {
            val eventType = call.parameters["eventType"]?.let { EventType.valueOf(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid event type"))

            val policy = retentionService.getPolicy(eventType)
            if (policy != null) {
                call.respond(HttpStatusCode.OK, policy)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Policy not found"))
            }
        }

        /**
         * Create or update retention policy
         */
        post("/policies") {
            try {
                val params = call.receiveParameters()
                val eventType = EventType.valueOf(params["eventType"] ?: "")
                val retentionDays = params["retentionDays"]?.toInt() ?: 365
                val autoDelete = params["autoDelete"]?.toBoolean() ?: false
                val description = params["description"] ?: ""

                val policy = retentionService.createRetentionPolicy(
                    eventType,
                    retentionDays,
                    autoDelete,
                    description
                )

                call.respond(HttpStatusCode.Created, policy)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            }
        }

        /**
         * Apply retention policies (cleanup old data)
         */
        post("/apply") {
            val dryRun = call.request.queryParameters["dryRun"]?.toBoolean() ?: false

            if (dryRun) {
                val expiredEvents = retentionService.getExpiredEvents(true)
                call.respond(
                    HttpStatusCode.OK, mapOf(
                        "dryRun" to true,
                        "expiredEvents" to expiredEvents,
                        "totalExpired" to expiredEvents.values.sumOf { it.size }
                    )
                )
            } else {
                val deletedCounts = retentionService.applyRetentionPolicies()
                call.respond(
                    HttpStatusCode.OK, mapOf(
                        "applied" to true,
                        "deletedByEventType" to deletedCounts,
                        "totalDeleted" to deletedCounts.values.sum()
                    )
                )
            }
        }

        /**
         * Initialize default retention policies
         */
        post("/initialize") {
            retentionService.initializeDefaultPolicies()
            call.respond(
                HttpStatusCode.OK,
                mapOf("message" to "Default retention policies initialized")
            )
        }
    }
}
