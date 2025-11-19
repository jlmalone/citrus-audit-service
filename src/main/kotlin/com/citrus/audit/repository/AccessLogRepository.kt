package com.citrus.audit.repository

import com.citrus.audit.domain.AccessLog
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class AccessLogRepository {

    fun create(log: AccessLog): AccessLog = transaction {
        AccessLogs.insert {
            it[id] = log.id
            it[userId] = log.userId
            it[timestamp] = log.timestamp.toJavaInstant()
            it[endpoint] = log.endpoint
            it[method] = log.method
            it[ipAddress] = log.ipAddress
            it[userAgent] = log.userAgent
            it[statusCode] = log.statusCode
            it[responseTime] = log.responseTime
            it[requestSize] = log.requestSize
            it[responseSize] = log.responseSize
        }
        log
    }

    fun findById(id: String): AccessLog? = transaction {
        AccessLogs.select { AccessLogs.id eq id }
            .mapNotNull { toAccessLog(it) }
            .singleOrNull()
    }

    fun findByUserId(userId: String, limit: Int = 100): List<AccessLog> = transaction {
        AccessLogs.select { AccessLogs.userId eq userId }
            .orderBy(AccessLogs.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { toAccessLog(it) }
    }

    fun findByIpAddress(ipAddress: String, limit: Int = 100): List<AccessLog> = transaction {
        AccessLogs.select { AccessLogs.ipAddress eq ipAddress }
            .orderBy(AccessLogs.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { toAccessLog(it) }
    }

    fun findFailedAttempts(limit: Int = 100): List<AccessLog> = transaction {
        AccessLogs.select { AccessLogs.statusCode greaterEq 400 }
            .orderBy(AccessLogs.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { toAccessLog(it) }
    }

    fun countFailedAttempts(start: Instant, end: Instant): Long = transaction {
        AccessLogs.select {
            (AccessLogs.statusCode greaterEq 400) and
            (AccessLogs.timestamp greaterEq start.toJavaInstant()) and
            (AccessLogs.timestamp lessEq end.toJavaInstant())
        }.count()
    }

    fun deleteOlderThan(cutoffDate: Instant): Int = transaction {
        AccessLogs.deleteWhere { timestamp less cutoffDate.toJavaInstant() }
    }

    private fun toAccessLog(row: ResultRow): AccessLog {
        return AccessLog(
            id = row[AccessLogs.id],
            userId = row[AccessLogs.userId],
            timestamp = row[AccessLogs.timestamp].toKotlinInstant(),
            endpoint = row[AccessLogs.endpoint],
            method = row[AccessLogs.method],
            ipAddress = row[AccessLogs.ipAddress],
            userAgent = row[AccessLogs.userAgent],
            statusCode = row[AccessLogs.statusCode],
            responseTime = row[AccessLogs.responseTime],
            requestSize = row[AccessLogs.requestSize],
            responseSize = row[AccessLogs.responseSize]
        )
    }
}
