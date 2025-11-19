package com.citrus.audit.repository

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.AccessLog
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class AccessLogRepositoryTest {

    private val repository = AccessLogRepository()

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should create and find access log by id`() {
        val log = createTestAccessLog("log-1", "user-123")

        repository.create(log)
        val found = repository.findById("log-1")

        assertNotNull(found)
        assertEquals(log.id, found.id)
        assertEquals(log.userId, found.userId)
        assertEquals(log.endpoint, found.endpoint)
        assertEquals(log.statusCode, found.statusCode)
    }

    @Test
    fun `should find access logs by user id`() {
        val log1 = createTestAccessLog("log-1", "user-123")
        val log2 = createTestAccessLog("log-2", "user-123")
        val log3 = createTestAccessLog("log-3", "user-456")

        repository.create(log1)
        repository.create(log2)
        repository.create(log3)

        val userLogs = repository.findByUserId("user-123")
        assertEquals(2, userLogs.size)
    }

    @Test
    fun `should find access logs by IP address`() {
        val log1 = createTestAccessLog("log-1", "user-123").copy(ipAddress = "192.168.1.1")
        val log2 = createTestAccessLog("log-2", "user-456").copy(ipAddress = "192.168.1.1")
        val log3 = createTestAccessLog("log-3", "user-789").copy(ipAddress = "10.0.0.1")

        repository.create(log1)
        repository.create(log2)
        repository.create(log3)

        val ipLogs = repository.findByIpAddress("192.168.1.1")
        assertEquals(2, ipLogs.size)
    }

    @Test
    fun `should find failed access attempts`() {
        val log1 = createTestAccessLog("log-1", "user-123").copy(statusCode = 200)
        val log2 = createTestAccessLog("log-2", "user-456").copy(statusCode = 401)
        val log3 = createTestAccessLog("log-3", "user-789").copy(statusCode = 403)
        val log4 = createTestAccessLog("log-4", "user-999").copy(statusCode = 500)

        repository.create(log1)
        repository.create(log2)
        repository.create(log3)
        repository.create(log4)

        val failedLogs = repository.findFailedAttempts()
        assertEquals(3, failedLogs.size)
    }

    @Test
    fun `should count failed attempts in time range`() {
        val now = Clock.System.now()
        val start = now.minus(1.days)
        val end = now.plus(1.days)

        val log1 = createTestAccessLog("log-1", "user-123").copy(statusCode = 401)
        val log2 = createTestAccessLog("log-2", "user-456").copy(statusCode = 403)

        repository.create(log1)
        repository.create(log2)

        val count = repository.countFailedAttempts(start, end)
        assertEquals(2, count)
    }

    @Test
    fun `should delete old access logs`() {
        val now = Clock.System.now()
        val old = now.minus(100.days)

        val oldLog = createTestAccessLog("log-old", "user-123").copy(timestamp = old)
        val newLog = createTestAccessLog("log-new", "user-123").copy(timestamp = now)

        repository.create(oldLog)
        repository.create(newLog)

        val deleted = repository.deleteOlderThan(now.minus(50.days))
        assertEquals(1, deleted)

        assertNull(repository.findById("log-old"))
        assertNotNull(repository.findById("log-new"))
    }

    @Test
    fun `should handle access logs with optional fields`() {
        val log = AccessLog(
            id = "log-optional",
            userId = "user-123",
            timestamp = Clock.System.now(),
            endpoint = "/api/test",
            method = "GET",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0",
            statusCode = 200,
            responseTime = 150,
            requestSize = null,
            responseSize = null
        )

        repository.create(log)
        val found = repository.findById("log-optional")

        assertNotNull(found)
        assertNull(found.requestSize)
        assertNull(found.responseSize)
    }

    @Test
    fun `should return null for non-existent access log`() {
        val found = repository.findById("non-existent")
        assertNull(found)
    }

    private fun createTestAccessLog(id: String, userId: String): AccessLog {
        return AccessLog(
            id = id,
            userId = userId,
            timestamp = Clock.System.now(),
            endpoint = "/api/test",
            method = "GET",
            ipAddress = "192.168.1.1",
            userAgent = "Mozilla/5.0 Test",
            statusCode = 200,
            responseTime = 100,
            requestSize = 1024,
            responseSize = 2048
        )
    }
}
