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
import kotlin.time.Duration.Companion.days

class ComplianceRoutesTest {

    @BeforeEach
    fun setup() {
        DatabaseConfig.initForTesting()
    }

    @Test
    fun `should generate compliance report`() = testApplication {
        application {
            module()
        }

        val now = Clock.System.now()
        val start = now.minus(30.days)

        val response = client.post("/api/compliance/reports") {
            contentType(ContentType.Application.Json)
            setBody("""{
                "reportType": "SOC2_TYPE1",
                "periodStart": "$start",
                "periodEnd": "$now"
            }""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("SOC2_TYPE1"))
    }

    @Test
    fun `should get all compliance reports`() = testApplication {
        application {
            module()
        }

        val now = Clock.System.now()
        val start = now.minus(30.days)

        // Create a report first
        client.post("/api/compliance/reports") {
            contentType(ContentType.Application.Json)
            setBody("""{
                "reportType": "GDPR_COMPLIANCE",
                "periodStart": "$start",
                "periodEnd": "$now"
            }""")
        }

        val response = client.get("/api/compliance/reports")
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `should get reports by type`() = testApplication {
        application {
            module()
        }

        val now = Clock.System.now()
        val start = now.minus(30.days)

        // Create a report
        client.post("/api/compliance/reports") {
            contentType(ContentType.Application.Json)
            setBody("""{
                "reportType": "DATA_RETENTION",
                "periodStart": "$start",
                "periodEnd": "$now"
            }""")
        }

        val response = client.get("/api/compliance/reports/type/DATA_RETENTION")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("DATA_RETENTION"))
    }

    @Test
    fun `should return 400 for invalid report type`() = testApplication {
        application {
            module()
        }

        val response = client.get("/api/compliance/reports/type/INVALID_TYPE")
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
