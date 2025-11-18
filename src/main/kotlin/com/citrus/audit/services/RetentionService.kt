package com.citrus.audit.services

import com.citrus.audit.database.AuditEvents
import com.citrus.audit.database.DatabaseFactory.dbQuery
import com.citrus.audit.database.RetentionPolicies
import com.citrus.audit.models.EventType
import com.citrus.audit.models.RetentionPolicy
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.*
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class RetentionService(private val config: ApplicationConfig) {

    private val defaultRetentionDays = config.property("retention.defaultDays").getString().toInt()
    private val gdprRetentionDays = config.property("retention.gdprDays").getString().toInt()

    /**
     * Create or update a retention policy
     */
    suspend fun createRetentionPolicy(
        eventType: EventType,
        retentionDays: Int,
        autoDelete: Boolean,
        description: String
    ): RetentionPolicy = dbQuery {
        val now = Instant.now()
        val policyId = UUID.randomUUID()

        // Check if policy exists
        val existing = RetentionPolicies.selectAll()
            .where { RetentionPolicies.eventType eq eventType.name }
            .singleOrNull()

        if (existing != null) {
            // Update existing
            RetentionPolicies.update({ RetentionPolicies.eventType eq eventType.name }) {
                it[RetentionPolicies.retentionDays] = retentionDays
                it[RetentionPolicies.autoDelete] = autoDelete
                it[RetentionPolicies.description] = description
                it[updatedAt] = now
            }

            RetentionPolicy(
                id = existing[RetentionPolicies.id].toString(),
                eventType = eventType.name,
                retentionDays = retentionDays,
                autoDelete = autoDelete,
                description = description
            )
        } else {
            // Create new
            RetentionPolicies.insert {
                it[id] = policyId
                it[RetentionPolicies.eventType] = eventType.name
                it[RetentionPolicies.retentionDays] = retentionDays
                it[RetentionPolicies.autoDelete] = autoDelete
                it[RetentionPolicies.description] = description
                it[createdAt] = now
                it[updatedAt] = now
            }

            RetentionPolicy(
                id = policyId.toString(),
                eventType = eventType.name,
                retentionDays = retentionDays,
                autoDelete = autoDelete,
                description = description
            )
        }
    }

    /**
     * Get all retention policies
     */
    suspend fun getAllPolicies(): List<RetentionPolicy> = dbQuery {
        RetentionPolicies.selectAll()
            .map { row ->
                RetentionPolicy(
                    id = row[RetentionPolicies.id].toString(),
                    eventType = row[RetentionPolicies.eventType],
                    retentionDays = row[RetentionPolicies.retentionDays],
                    autoDelete = row[RetentionPolicies.autoDelete],
                    description = row[RetentionPolicies.description]
                )
            }
    }

    /**
     * Get retention policy for an event type
     */
    suspend fun getPolicy(eventType: EventType): RetentionPolicy? = dbQuery {
        RetentionPolicies.selectAll()
            .where { RetentionPolicies.eventType eq eventType.name }
            .map { row ->
                RetentionPolicy(
                    id = row[RetentionPolicies.id].toString(),
                    eventType = row[RetentionPolicies.eventType],
                    retentionDays = row[RetentionPolicies.retentionDays],
                    autoDelete = row[RetentionPolicies.autoDelete],
                    description = row[RetentionPolicies.description]
                )
            }
            .singleOrNull()
    }

    /**
     * Apply retention policies and delete old events
     */
    suspend fun applyRetentionPolicies(): Map<String, Int> = dbQuery {
        val policies = RetentionPolicies.selectAll()
            .where { RetentionPolicies.autoDelete eq true }
            .toList()

        val deletedCounts = mutableMapOf<String, Int>()

        for (policy in policies) {
            val eventType = policy[RetentionPolicies.eventType]
            val retentionDays = policy[RetentionPolicies.retentionDays]
            val cutoffDate = Instant.now().minus(retentionDays.toLong(), ChronoUnit.DAYS)

            val deleted = AuditEvents.deleteWhere {
                (AuditEvents.eventType eq eventType) and (AuditEvents.timestamp less cutoffDate)
            }

            if (deleted > 0) {
                deletedCounts[eventType] = deleted
            }
        }

        // Also apply default retention for events without specific policies
        val defaultCutoffDate = Instant.now().minus(defaultRetentionDays.toLong(), ChronoUnit.DAYS)
        val policiedEventTypes = policies.map { it[RetentionPolicies.eventType] }

        if (policiedEventTypes.isNotEmpty()) {
            val deletedDefault = AuditEvents.deleteWhere {
                (eventType notInList policiedEventTypes) and (timestamp less defaultCutoffDate)
            }

            if (deletedDefault > 0) {
                deletedCounts["default"] = deletedDefault
            }
        }

        deletedCounts
    }

    /**
     * Get events that will be deleted by retention policy
     */
    suspend fun getExpiredEvents(dryRun: Boolean = true): Map<String, List<String>> = dbQuery {
        val policies = RetentionPolicies.selectAll().toList()
        val expiredEvents = mutableMapOf<String, MutableList<String>>()

        for (policy in policies) {
            val eventType = policy[RetentionPolicies.eventType]
            val retentionDays = policy[RetentionPolicies.retentionDays]
            val cutoffDate = Instant.now().minus(retentionDays.toLong(), ChronoUnit.DAYS)

            val events = AuditEvents.selectAll()
                .where { (AuditEvents.eventType eq eventType) and (AuditEvents.timestamp less cutoffDate) }
                .map { it[AuditEvents.id].toString() }

            if (events.isNotEmpty()) {
                expiredEvents[eventType] = events.toMutableList()
            }
        }

        expiredEvents
    }

    /**
     * Initialize default retention policies
     */
    suspend fun initializeDefaultPolicies() {
        // Login events - 90 days
        createRetentionPolicy(
            EventType.USER_LOGIN,
            90,
            autoDelete = true,
            "User login events retained for 90 days"
        )

        // Resource access - 365 days (1 year)
        createRetentionPolicy(
            EventType.RESOURCE_ACCESSED,
            365,
            autoDelete = true,
            "Resource access events retained for 1 year"
        )

        // Critical events - GDPR maximum (7 years)
        createRetentionPolicy(
            EventType.GDPR_ERASURE_COMPLETED,
            gdprRetentionDays,
            autoDelete = false,
            "GDPR erasure events retained for 7 years, manual deletion only"
        )

        // Compliance reports - 7 years
        createRetentionPolicy(
            EventType.COMPLIANCE_REPORT_GENERATED,
            gdprRetentionDays,
            autoDelete = false,
            "Compliance reports retained for 7 years"
        )
    }
}
