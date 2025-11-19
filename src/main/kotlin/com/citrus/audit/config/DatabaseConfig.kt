package com.citrus.audit.config

import com.citrus.audit.repository.*
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseConfig {

    fun init(
        jdbcUrl: String = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/citrus_audit",
        username: String = System.getenv("DATABASE_USER") ?: "postgres",
        password: String = System.getenv("DATABASE_PASSWORD") ?: "postgres"
    ) {
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        // Create tables
        transaction {
            SchemaUtils.create(
                AuditEvents,
                UserActivities,
                AccessLogs,
                ComplianceReports,
                DataRetentionPolicies,
                GdprErasureRequests
            )
        }
    }

    fun initForTesting() {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
            driverClassName = "org.h2.Driver"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        // Create tables
        transaction {
            SchemaUtils.create(
                AuditEvents,
                UserActivities,
                AccessLogs,
                ComplianceReports,
                DataRetentionPolicies,
                GdprErasureRequests
            )
        }
    }
}
