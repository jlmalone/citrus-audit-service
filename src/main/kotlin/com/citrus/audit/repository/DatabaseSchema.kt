package com.citrus.audit.repository

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object AuditEvents : Table("audit_events") {
    val id = varchar("id", 100)
    val timestamp = timestamp("timestamp")
    val eventType = varchar("event_type", 50)
    val userId = varchar("user_id", 100)
    val action = varchar("action", 200)
    val resourceType = varchar("resource_type", 100)
    val resourceId = varchar("resource_id", 100)
    val ipAddress = varchar("ip_address", 50)
    val userAgent = text("user_agent")
    val metadata = text("metadata")
    val status = varchar("status", 20)

    override val primaryKey = PrimaryKey(id)
}

object UserActivities : Table("user_activities") {
    val id = varchar("id", 100)
    val userId = varchar("user_id", 100)
    val sessionId = varchar("session_id", 100)
    val activityType = varchar("activity_type", 50)
    val timestamp = timestamp("timestamp")
    val duration = long("duration").nullable()
    val details = text("details").nullable()

    override val primaryKey = PrimaryKey(id)
}

object AccessLogs : Table("access_logs") {
    val id = varchar("id", 100)
    val userId = varchar("user_id", 100)
    val timestamp = timestamp("timestamp")
    val endpoint = varchar("endpoint", 500)
    val method = varchar("method", 10)
    val ipAddress = varchar("ip_address", 50)
    val userAgent = text("user_agent")
    val statusCode = integer("status_code")
    val responseTime = long("response_time")
    val requestSize = long("request_size").nullable()
    val responseSize = long("response_size").nullable()

    override val primaryKey = PrimaryKey(id)
}

object ComplianceReports : Table("compliance_reports") {
    val id = varchar("id", 100)
    val reportType = varchar("report_type", 50)
    val generatedAt = timestamp("generated_at")
    val periodStart = timestamp("period_start")
    val periodEnd = timestamp("period_end")
    val summary = text("summary")
    val findings = text("findings")

    override val primaryKey = PrimaryKey(id)
}

object DataRetentionPolicies : Table("data_retention_policies") {
    val id = varchar("id", 100)
    val name = varchar("name", 200)
    val description = text("description")
    val dataType = varchar("data_type", 50)
    val retentionPeriodDays = integer("retention_period_days")
    val autoDelete = bool("auto_delete")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val isActive = bool("is_active")

    override val primaryKey = PrimaryKey(id)
}

object GdprErasureRequests : Table("gdpr_erasure_requests") {
    val id = varchar("id", 100)
    val userId = varchar("user_id", 100)
    val requestedAt = timestamp("requested_at")
    val status = varchar("status", 20)
    val completedAt = timestamp("completed_at").nullable()
    val deletedRecords = text("deleted_records")

    override val primaryKey = PrimaryKey(id)
}
