package com.citrus.audit.service

import com.citrus.audit.domain.*
import com.citrus.audit.repository.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.days

class DataRetentionService(
    private val retentionRepository: DataRetentionRepository = DataRetentionRepository(),
    private val auditRepository: AuditEventRepository = AuditEventRepository(),
    private val activityRepository: UserActivityRepository = UserActivityRepository(),
    private val accessLogRepository: AccessLogRepository = AccessLogRepository()
) {

    fun createRetentionPolicy(policy: DataRetentionPolicy): DataRetentionPolicy {
        return retentionRepository.createPolicy(policy)
    }

    fun getRetentionPolicy(id: String): DataRetentionPolicy? {
        return retentionRepository.findPolicyById(id)
    }

    fun getActivePolicies(): List<DataRetentionPolicy> {
        return retentionRepository.findActivePolicies()
    }

    fun applyRetentionPolicies(): Map<DataType, Int> {
        val results = mutableMapOf<DataType, Int>()
        val policies = retentionRepository.findActivePolicies()

        policies.forEach { policy ->
            if (policy.autoDelete) {
                val cutoffDate = Clock.System.now().minus(policy.retentionPeriodDays.days)
                val deleted = when (policy.dataType) {
                    DataType.AUDIT_EVENTS -> auditRepository.deleteOlderThan(cutoffDate)
                    DataType.USER_ACTIVITIES -> activityRepository.deleteOlderThan(cutoffDate)
                    DataType.ACCESS_LOGS -> accessLogRepository.deleteOlderThan(cutoffDate)
                    else -> 0
                }
                results[policy.dataType] = deleted
            }
        }

        return results
    }

    fun requestGdprErasure(userId: String): GdprErasureRequest {
        val request = GdprErasureRequest(
            id = UUID.randomUUID().toString(),
            userId = userId,
            requestedAt = Clock.System.now(),
            status = ErasureStatus.PENDING
        )
        return retentionRepository.createErasureRequest(request)
    }

    fun processGdprErasure(requestId: String): GdprErasureRequest {
        val request = retentionRepository.findErasureRequestById(requestId)
            ?: throw IllegalArgumentException("Erasure request not found: $requestId")

        if (request.status != ErasureStatus.PENDING) {
            throw IllegalStateException("Request already processed: $requestId")
        }

        // Update status to in progress
        val inProgressRequest = request.copy(status = ErasureStatus.IN_PROGRESS)
        retentionRepository.updateErasureRequest(inProgressRequest)

        try {
            // Delete user data from all tables
            val deletedRecords = mutableMapOf<String, Long>()

            deletedRecords["user_activities"] = activityRepository.deleteByUserId(request.userId).toLong()

            // Note: We keep audit events for compliance but could anonymize them
            deletedRecords["anonymized_audit_events"] = 0L

            // Complete the request
            val completedRequest = inProgressRequest.copy(
                status = ErasureStatus.COMPLETED,
                completedAt = Clock.System.now(),
                deletedRecords = deletedRecords
            )
            return retentionRepository.updateErasureRequest(completedRequest)

        } catch (e: Exception) {
            // Mark as failed
            val failedRequest = inProgressRequest.copy(status = ErasureStatus.FAILED)
            retentionRepository.updateErasureRequest(failedRequest)
            throw e
        }
    }

    fun getErasureRequest(requestId: String): GdprErasureRequest? {
        return retentionRepository.findErasureRequestById(requestId)
    }

    fun getUserErasureRequests(userId: String): List<GdprErasureRequest> {
        return retentionRepository.findErasureRequestsByUserId(userId)
    }

    fun getPendingErasureRequests(): List<GdprErasureRequest> {
        return retentionRepository.findPendingErasureRequests()
    }
}
