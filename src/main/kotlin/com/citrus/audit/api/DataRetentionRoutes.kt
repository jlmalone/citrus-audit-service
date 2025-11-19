package com.citrus.audit.api

import com.citrus.audit.domain.DataRetentionPolicy
import com.citrus.audit.service.DataRetentionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateRetentionPolicyRequest(
    val policy: DataRetentionPolicy
)

@Serializable
data class GdprErasureRequestDto(
    val userId: String
)

fun Route.dataRetentionRoutes(dataRetentionService: DataRetentionService = DataRetentionService()) {

    route("/api/retention") {

        post("/policies") {
            val request = call.receive<CreateRetentionPolicyRequest>()
            val policy = dataRetentionService.createRetentionPolicy(request.policy)
            call.respond(HttpStatusCode.Created, policy)
        }

        get("/policies/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing policy ID")
            )
            val policy = dataRetentionService.getRetentionPolicy(id)
            if (policy != null) {
                call.respond(policy)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Policy not found"))
            }
        }

        get("/policies") {
            val policies = dataRetentionService.getActivePolicies()
            call.respond(policies)
        }

        post("/policies/apply") {
            val results = dataRetentionService.applyRetentionPolicies()
            call.respond(mapOf("deleted" to results))
        }
    }

    route("/api/gdpr") {

        post("/erasure") {
            val request = call.receive<GdprErasureRequestDto>()
            val erasureRequest = dataRetentionService.requestGdprErasure(request.userId)
            call.respond(HttpStatusCode.Created, erasureRequest)
        }

        post("/erasure/{id}/process") {
            val id = call.parameters["id"] ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing request ID")
            )
            try {
                val erasureRequest = dataRetentionService.processGdprErasure(id)
                call.respond(erasureRequest)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to e.message))
            }
        }

        get("/erasure/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing request ID")
            )
            val erasureRequest = dataRetentionService.getErasureRequest(id)
            if (erasureRequest != null) {
                call.respond(erasureRequest)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Erasure request not found"))
            }
        }

        get("/erasure/user/{userId}") {
            val userId = call.parameters["userId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing user ID")
            )
            val requests = dataRetentionService.getUserErasureRequests(userId)
            call.respond(requests)
        }

        get("/erasure/pending") {
            val requests = dataRetentionService.getPendingErasureRequests()
            call.respond(requests)
        }
    }
}
