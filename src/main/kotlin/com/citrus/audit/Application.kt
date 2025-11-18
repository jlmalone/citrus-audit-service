package com.citrus.audit

import com.citrus.audit.database.DatabaseFactory
import com.citrus.audit.routes.*
import com.citrus.audit.services.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init(environment.config)

    // Configure JSON serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Configure CORS
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    // Configure logging
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.origin.uri.startsWith("/audit") }
    }

    // Configure status pages for error handling
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to "Internal server error",
                    "message" to (cause.message ?: "Unknown error")
                )
            )
        }
    }

    // Initialize services
    val auditService = AuditService()
    val complianceService = ComplianceService()
    val retentionService = RetentionService(environment.config)
    val gdprService = GdprService(auditService)

    // Configure routing
    routing {
        get("/") {
            call.respond(
                HttpStatusCode.OK,
                mapOf(
                    "service" to "Citrus Audit & Compliance Service",
                    "version" to "1.0.0",
                    "status" to "operational",
                    "endpoints" to listOf(
                        "/audit/events - Audit event management",
                        "/audit/access-logs - Access log queries",
                        "/audit/users/{userId}/activity - User activity tracking",
                        "/audit/receipts - Audit receipts",
                        "/compliance/reports/soc2 - SOC 2 compliance reports",
                        "/compliance/reports/gdpr - GDPR compliance reports",
                        "/compliance/stats - Compliance statistics",
                        "/retention/policies - Data retention policies",
                        "/retention/apply - Apply retention policies",
                        "/gdpr/erasure - GDPR erasure requests",
                        "/gdpr/users/{userId}/export - Export user data"
                    )
                )
            )
        }

        // Register all routes
        auditRoutes(auditService)
        complianceRoutes(complianceService, auditService)
        retentionRoutes(retentionService)
        gdprRoutes(gdprService)
    }

    log.info("🍊 Citrus Audit Service started successfully")
}
