package com.citrus.audit.api

import com.citrus.audit.config.DatabaseConfig
import com.citrus.audit.domain.EventType
import com.citrus.audit.module
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class AuditRoutesTest {

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should create audit event`() = testApplication {
        application {
            module()
        }

        val response = client.post("/api/audit/events") {
            contentType(ContentType.Application.Json)
            setBody("""{
                "eventType": "USER_LOGIN",
                "userId": "user-123",
                "action": "User logged in",
                "resourceType": "user",
                "resourceId": "user-123",
                "ipAddress": "192.168.1.1",
                "userAgent": "Mozilla/5.0",
                "metadata": {}
            }""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("user-123"))
    }

    @Test
    fun `should get audit event by id`() = testApplication {
        application {
            module()
        }

        // First create an event
        val createResponse = client.post("/api/audit/events") {
            contentType(ContentType.Application.Json)
            setBody("""{
                "eventType": "USER_CREATED",
                "userId": "user-456",
                "action": "User created",
                "resourceType": "user",
                "resourceId": "user-456",
                "ipAddress": "192.168.1.1",
                "userAgent": "Mozilla/5.0",
                "metadata": {}
            }""")
        }

        val eventId = Json.parseToJsonElement(createResponse.bodyAsText())
            .toString().substringAfter("\"id\":\"").substringBefore("\"")

        // Now get it
        val getResponse = client.get("/api/audit/events/$eventId")
        assertEquals(HttpStatusCode.OK, getResponse.status)
    }

    @Test
    fun `should get user events`() = testApplication {
        application {
            module()
        }

        // Create an event
        client.post("/api/audit/events") {
            contentType(ContentType.Application.Json)
            setBody("""{
                "eventType": "USER_LOGIN",
                "userId": "user-789",
                "action": "Login",
                "resourceType": "user",
                "resourceId": "user-789",
                "ipAddress": "192.168.1.1",
                "userAgent": "Mozilla/5.0",
                "metadata": {}
            }""")
        }

        val response = client.get("/api/audit/events/user/user-789")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("user-789"))
    }

    @Test
    fun `should return 404 for non-existent event`() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/audit/events/non-existent-id")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
