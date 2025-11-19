package com.citrus.audit.repository

import com.citrus.audit.domain.*
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class DataRetentionRepository {

    fun createPolicy(policy: DataRetentionPolicy): DataRetentionPolicy = transaction {
        DataRetentionPolicies.insert {
            it[id] = policy.id
            it[name] = policy.name
            it[description] = policy.description
            it[dataType] = policy.dataType.name
            it[retentionPeriodDays] = policy.retentionPeriodDays
            it[autoDelete] = policy.autoDelete
            it[createdAt] = policy.createdAt.toJavaInstant()
            it[updatedAt] = policy.updatedAt.toJavaInstant()
            it[isActive] = policy.isActive
        }
        policy
    }

    fun findPolicyById(id: String): DataRetentionPolicy? = transaction {
        DataRetentionPolicies.select { DataRetentionPolicies.id eq id }
            .mapNotNull { toDataRetentionPolicy(it) }
            .singleOrNull()
    }

    fun findActivePolicies(): List<DataRetentionPolicy> = transaction {
        DataRetentionPolicies.select { DataRetentionPolicies.isActive eq true }
            .map { toDataRetentionPolicy(it) }
    }

    fun findPolicyByDataType(dataType: DataType): DataRetentionPolicy? = transaction {
        DataRetentionPolicies.select {
            (DataRetentionPolicies.dataType eq dataType.name) and
            (DataRetentionPolicies.isActive eq true)
        }
        .mapNotNull { toDataRetentionPolicy(it) }
        .singleOrNull()
    }

    fun createErasureRequest(request: GdprErasureRequest): GdprErasureRequest = transaction {
        GdprErasureRequests.insert {
            it[id] = request.id
            it[userId] = request.userId
            it[requestedAt] = request.requestedAt.toJavaInstant()
            it[status] = request.status.name
            it[completedAt] = request.completedAt?.toJavaInstant()
            it[deletedRecords] = Json.encodeToString(request.deletedRecords)
        }
        request
    }

    fun updateErasureRequest(request: GdprErasureRequest): GdprErasureRequest = transaction {
        GdprErasureRequests.update({ GdprErasureRequests.id eq request.id }) {
            it[status] = request.status.name
            it[completedAt] = request.completedAt?.toJavaInstant()
            it[deletedRecords] = Json.encodeToString(request.deletedRecords)
        }
        request
    }

    fun findErasureRequestById(id: String): GdprErasureRequest? = transaction {
        GdprErasureRequests.select { GdprErasureRequests.id eq id }
            .mapNotNull { toGdprErasureRequest(it) }
            .singleOrNull()
    }

    fun findErasureRequestsByUserId(userId: String): List<GdprErasureRequest> = transaction {
        GdprErasureRequests.select { GdprErasureRequests.userId eq userId }
            .orderBy(GdprErasureRequests.requestedAt, SortOrder.DESC)
            .map { toGdprErasureRequest(it) }
    }

    fun findPendingErasureRequests(): List<GdprErasureRequest> = transaction {
        GdprErasureRequests.select { GdprErasureRequests.status eq ErasureStatus.PENDING.name }
            .orderBy(GdprErasureRequests.requestedAt, SortOrder.ASC)
            .map { toGdprErasureRequest(it) }
    }

    private fun toDataRetentionPolicy(row: ResultRow): DataRetentionPolicy {
        return DataRetentionPolicy(
            id = row[DataRetentionPolicies.id],
            name = row[DataRetentionPolicies.name],
            description = row[DataRetentionPolicies.description],
            dataType = DataType.valueOf(row[DataRetentionPolicies.dataType]),
            retentionPeriodDays = row[DataRetentionPolicies.retentionPeriodDays],
            autoDelete = row[DataRetentionPolicies.autoDelete],
            createdAt = row[DataRetentionPolicies.createdAt].toKotlinInstant(),
            updatedAt = row[DataRetentionPolicies.updatedAt].toKotlinInstant(),
            isActive = row[DataRetentionPolicies.isActive]
        )
    }

    private fun toGdprErasureRequest(row: ResultRow): GdprErasureRequest {
        return GdprErasureRequest(
            id = row[GdprErasureRequests.id],
            userId = row[GdprErasureRequests.userId],
            requestedAt = row[GdprErasureRequests.requestedAt].toKotlinInstant(),
            status = ErasureStatus.valueOf(row[GdprErasureRequests.status]),
            completedAt = row[GdprErasureRequests.completedAt]?.toKotlinInstant(),
            deletedRecords = Json.decodeFromString(row[GdprErasureRequests.deletedRecords])
        )
    }
}
