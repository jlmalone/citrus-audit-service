package com.citrus.audit.api

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.datetime.Clock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DataRetentionRoutesTest {

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should create retention policy`() = testApplication {
        application {
            module()
        }

        val now = Clock.System.now()

        val response = client.post("/api/retention/policies") {
            contentType(ContentType.Application.Json)
            setBody("""{
                "policy": {
                    "id": "policy-1",
                    "name": "Test Policy",
                    "description": "Test Description",
                    "dataType": "AUDIT_EVENTS",
                    "retentionPeriodDays": 90,
                    "autoDelete": true,
                    "createdAt": "$now",
                    "updatedAt": "$now",
                    "isActive": true
                }
            }""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("policy-1"))
    }

    @Test
    fun `should get active policies`() = testApplication {
        application {
            module()
        }

        val now = Clock.System.now()

        // Create a policy first
        client.post("/api/retention/policies") {
            contentType(ContentType.Application.Json)
            setBody("""{
                "policy": {
                    "id": "policy-2",
                    "name": "Active Policy",
                    "description": "Test",
                    "dataType": "USER_ACTIVITIES",
                    "retentionPeriodDays": 60,
                    "autoDelete": true,
                    "createdAt": "$now",
                    "updatedAt": "$now",
                    "isActive": true
                }
            }""")
        }

        val response = client.get("/api/retention/policies")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `should request GDPR erasure`() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/gdpr/erasure") {
            contentType(ContentType.Application.Json)
            setBody("""{
                "userId": "user-123"
            }""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("user-123"))
        assertTrue(response.bodyAsText().contains("PENDING"))
    }

    @Test
    fun `should get pending erasure requests`() = testApplication {
        application {
            module()
        }

        // Create an erasure request
        client.post("/api/gdpr/erasure") {
            contentType(ContentType.Application.Json)
            setBody("""{
                "userId": "user-456"
            }""")
        }

        val response = client.get("/api/gdpr/erasure/pending")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `should get user erasure requests`() = testApplication {
        application {
            module()
        }

        // Create an erasure request
        client.post("/api/gdpr/erasure") {
            contentType(ContentType.Application.Json)
            setBody("""{
                "userId": "user-789"
            }""")
        }

        val response = client.get("/api/gdpr/erasure/user/user-789")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("user-789"))
    }
}
