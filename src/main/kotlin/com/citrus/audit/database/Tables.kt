package com.citrus.audit.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Audit events table - Event sourcing store
 */
object AuditEvents : Table("audit_events") {
    val id = uuid("id")
    val eventType = varchar("event_type", 50)
    val timestamp = timestamp("timestamp")
    val userId = varchar("user_id", 255).nullable()
    val username = varchar("username", 255).nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val resourceType = varchar("resource_type", 100).nullable()
    val resourceId = varchar("resource_id", 255).nullable()
    val action = varchar("action", 255)
    val description = text("description")
    val severity = varchar("severity", 20)
    val metadata = text("metadata") // JSON stored as text
    val success = bool("success")
    val errorMessage = text("error_message").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, userId, timestamp)
        index(isUnique = false, eventType, timestamp)
        index(isUnique = false, timestamp)
        index(isUnique = false, resourceType, resourceId)
    }
}

/**
 * Retention policies table
 */
object RetentionPolicies : Table("retention_policies") {
    val id = uuid("id")
    val eventType = varchar("event_type", 50)
    val retentionDays = integer("retention_days")
    val autoDelete = bool("auto_delete")
    val description = text("description")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = true, eventType)
    }
}

/**
 * GDPR erasure requests table
 */
object GdprErasureRequests : Table("gdpr_erasure_requests") {
    val id = uuid("id")
    val userId = varchar("user_id", 255)
    val requestedBy = varchar("requested_by", 255)
    val reason = text("reason")
    val status = varchar("status", 50)
    val eventsErased = integer("events_erased").nullable()
    val requestedAt = timestamp("requested_at")
    val completedAt = timestamp("completed_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(isUnique = false, userId)
        index(isUnique = false, status)
    }
}
