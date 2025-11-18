package com.citrus.audit.routes

import com.citrus.audit.models.GdprErasureRequest
import com.citrus.audit.services.GdprService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.gdprRoutes(gdprService: GdprService) {

    route("/gdpr") {

        /**
         * Request user data erasure
         */
        post("/erasure") {
            try {
                val params = call.receiveParameters()
                val request = GdprErasureRequest(
                    userId = params["userId"] ?: throw IllegalArgumentException("userId required"),
                    requestedBy = params["requestedBy"] ?: throw IllegalArgumentException("requestedBy required"),
                    reason = params["reason"] ?: "User requested data deletion"
                )

                val response = gdprService.requestErasure(request)
                call.respond(HttpStatusCode.Accepted, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
            }
        }

        /**
         * Execute GDPR erasure
         */
        post("/erasure/{requestId}/execute") {
            val requestId = call.parameters["requestId"]?.let { UUID.fromString(it) }
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request ID"))

            try {
                val response = gdprService.executeErasure(requestId)
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Execution failed")))
            }
        }

        /**
         * Get erasure request status
         */
        get("/erasure/{requestId}") {
            val requestId = call.parameters["requestId"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request ID"))

            val status = gdprService.getErasureStatus(requestId)
            if (status != null) {
                call.respond(HttpStatusCode.OK, status)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Request not found"))
            }
        }

        /**
         * Get all erasure requests
         */
        get("/erasure") {
            val status = call.request.queryParameters["status"]
            val requests = gdprService.getAllErasureRequests(status)
            call.respond(HttpStatusCode.OK, requests)
        }

        /**
         * Get user's erasure requests
         */
        get("/users/{userId}/erasure") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User ID required"))

            val requests = gdprService.getUserErasureRequests(userId)
            call.respond(HttpStatusCode.OK, requests)
        }

        /**
         * Export user data (GDPR data portability)
         */
        get("/users/{userId}/export") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "User ID required"))

            val userData = gdprService.exportUserData(userId)
            call.respond(HttpStatusCode.OK, userData)
        }
    }
}
