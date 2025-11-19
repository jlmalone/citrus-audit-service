package com.citrus.audit.repository

import com.citrus.audit.domain.*
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class ComplianceReportRepository {

    fun create(report: ComplianceReport): ComplianceReport = transaction {
        ComplianceReports.insert {
            it[id] = report.id
            it[reportType] = report.reportType.name
            it[generatedAt] = report.generatedAt.toJavaInstant()
            it[periodStart] = report.periodStart.toJavaInstant()
            it[periodEnd] = report.periodEnd.toJavaInstant()
            it[summary] = Json.encodeToString(report.summary)
            it[findings] = Json.encodeToString(report.findings)
        }
        report
    }

    fun findById(id: String): ComplianceReport? = transaction {
        ComplianceReports.select { ComplianceReports.id eq id }
            .mapNotNull { toComplianceReport(it) }
            .singleOrNull()
    }

    fun findByReportType(reportType: ReportType, limit: Int = 50): List<ComplianceReport> = transaction {
        ComplianceReports.select { ComplianceReports.reportType eq reportType.name }
            .orderBy(ComplianceReports.generatedAt, SortOrder.DESC)
            .limit(limit)
            .map { toComplianceReport(it) }
    }

    fun findAll(limit: Int = 50): List<ComplianceReport> = transaction {
        ComplianceReports.selectAll()
            .orderBy(ComplianceReports.generatedAt, SortOrder.DESC)
            .limit(limit)
            .map { toComplianceReport(it) }
    }

    private fun toComplianceReport(row: ResultRow): ComplianceReport {
        return ComplianceReport(
            id = row[ComplianceReports.id],
            reportType = ReportType.valueOf(row[ComplianceReports.reportType]),
            generatedAt = row[ComplianceReports.generatedAt].toKotlinInstant(),
            periodStart = row[ComplianceReports.periodStart].toKotlinInstant(),
            periodEnd = row[ComplianceReports.periodEnd].toKotlinInstant(),
            summary = Json.decodeFromString(row[ComplianceReports.summary]),
            findings = Json.decodeFromString(row[ComplianceReports.findings])
        )
    }
}
