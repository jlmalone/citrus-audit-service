package com.citrus.audit.api

import com.citrus.audit.domain.ReportType
import com.citrus.audit.service.ComplianceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class GenerateComplianceReportRequest(
    val reportType: ReportType,
    val periodStart: Instant,
    val periodEnd: Instant
)

fun Route.complianceRoutes(complianceService: ComplianceService = ComplianceService()) {

    route("/api/compliance") {

        post("/reports") {
            val request = call.receive<GenerateComplianceReportRequest>()
            val report = complianceService.generateComplianceReport(
                reportType = request.reportType,
                periodStart = request.periodStart,
                periodEnd = request.periodEnd
            )
            call.respond(HttpStatusCode.Created, report)
        }

        get("/reports/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing report ID")
            )
            val report = complianceService.getReportById(id)
            if (report != null) {
                call.respond(report)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Report not found"))
            }
        }

        get("/reports") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val reports = complianceService.getAllReports(limit)
            call.respond(reports)
        }

        get("/reports/type/{reportType}") {
            val reportTypeStr = call.parameters["reportType"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing report type")
            )
            try {
                val reportType = ReportType.valueOf(reportTypeStr)
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val reports = complianceService.getReportsByType(reportType, limit)
                call.respond(reports)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid report type"))
            }
        }
    }
}
