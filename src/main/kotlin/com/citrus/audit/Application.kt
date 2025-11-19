package com.citrus.audit

import com.citrus.audit.api.*
import com.citrus.audit.config.DatabaseConfig
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Initialize database
    DatabaseConfig.init()

    // Configure JSON serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Configure status pages
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(
                text = "500: $cause",
                status = HttpStatusCode.InternalServerError
            )
        }
    }

    // Configure routing
    routing {
        get("/") {
            call.respondText("Citrus Audit & Compliance Service")
        }

        get("/health") {
            call.respond(mapOf("status" to "healthy"))
        }

        auditRoutes()
        complianceRoutes()
        dataRetentionRoutes()
    }
}
