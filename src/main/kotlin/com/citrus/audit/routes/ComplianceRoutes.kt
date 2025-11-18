package com.citrus.audit.routes

import com.citrus.audit.services.AuditService
import com.citrus.audit.services.ComplianceService
import com.citrus.audit.models.EventType
import com.citrus.audit.models.EventSeverity
import com.citrus.audit.models.AuditEvent
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.Instant
import java.time.temporal.ChronoUnit

fun Route.complianceRoutes(complianceService: ComplianceService, auditService: AuditService) {

    route("/compliance") {

        /**
         * Generate SOC 2 compliance report
         */
        get("/reports/soc2") {
            val days = call.request.queryParameters["days"]?.toInt() ?: 30
            val endTime = Instant.now()
            val startTime = endTime.minus(days.toLong(), ChronoUnit.DAYS)

            val report = complianceService.generateSoc2Report(startTime, endTime)

            // Log report generation
            auditService.logEvent(
                AuditEvent(
                    eventType = EventType.COMPLIANCE_REPORT_GENERATED,
                    userId = call.request.queryParameters["userId"] ?: "system",
                    username = call.request.queryParameters["username"] ?: "System",
                    ipAddress = call.request.origin.remoteHost,
                    userAgent = call.request.headers["User-Agent"],
                    resourceType = "compliance_report",
                    resourceId = report.id,
                    action = "GENERATE_SOC2_REPORT",
                    description = "SOC 2 compliance report generated for $days day period",
                    severity = EventSeverity.INFO,
                    metadata = mapOf(
                        "reportId" to report.id,
                        "reportType" to "SOC2",
                        "periodDays" to days.toString()
                    )
                )
            )

            call.respond(HttpStatusCode.OK, report)
        }

        /**
         * Generate GDPR compliance report
         */
        get("/reports/gdpr") {
            val days = call.request.queryParameters["days"]?.toInt() ?: 30
            val endTime = Instant.now()
            val startTime = endTime.minus(days.toLong(), ChronoUnit.DAYS)

            val report = complianceService.generateGdprReport(startTime, endTime)

            // Log report generation
            auditService.logEvent(
                AuditEvent(
                    eventType = EventType.COMPLIANCE_REPORT_GENERATED,
                    userId = call.request.queryParameters["userId"] ?: "system",
                    username = call.request.queryParameters["username"] ?: "System",
                    ipAddress = call.request.origin.remoteHost,
                    userAgent = call.request.headers["User-Agent"],
                    resourceType = "compliance_report",
                    resourceId = report.id,
                    action = "GENERATE_GDPR_REPORT",
                    description = "GDPR compliance report generated for $days day period",
                    severity = EventSeverity.INFO,
                    metadata = mapOf(
                        "reportId" to report.id,
                        "reportType" to "GDPR",
                        "periodDays" to days.toString()
                    )
                )
            )

            call.respond(HttpStatusCode.OK, report)
        }

        /**
         * Get compliance statistics
         */
        get("/stats") {
            val days = call.request.queryParameters["days"]?.toInt() ?: 30
            val stats = complianceService.getComplianceStats(days)
            call.respond(HttpStatusCode.OK, stats)
        }
    }
}
