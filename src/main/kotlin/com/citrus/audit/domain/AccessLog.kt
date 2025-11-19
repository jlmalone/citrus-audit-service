package com.citrus.audit.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class AccessLog(
    val id: String,
    val userId: String,
    val timestamp: Instant,
    val endpoint: String,
    val method: String,
    val ipAddress: String,
    val userAgent: String,
    val statusCode: Int,
    val responseTime: Long, // milliseconds
    val requestSize: Long? = null,
    val responseSize: Long? = null
)
