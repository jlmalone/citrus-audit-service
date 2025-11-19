package com.citrus.audit.repository

import com.citrus.audit.domain.ActivityType
import com.citrus.audit.domain.UserActivity
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toKotlinInstant
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class UserActivityRepository {

    fun create(activity: UserActivity): UserActivity = transaction {
        UserActivities.insert {
            it[id] = activity.id
            it[userId] = activity.userId
            it[sessionId] = activity.sessionId
            it[activityType] = activity.activityType.name
            it[timestamp] = activity.timestamp.toJavaInstant()
            it[duration] = activity.duration
            it[details] = activity.details
        }
        activity
    }

    fun findById(id: String): UserActivity? = transaction {
        UserActivities.select { UserActivities.id eq id }
            .mapNotNull { toUserActivity(it) }
            .singleOrNull()
    }

    fun findByUserId(userId: String, limit: Int = 100): List<UserActivity> = transaction {
        UserActivities.select { UserActivities.userId eq userId }
            .orderBy(UserActivities.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { toUserActivity(it) }
    }

    fun findBySessionId(sessionId: String): List<UserActivity> = transaction {
        UserActivities.select { UserActivities.sessionId eq sessionId }
            .orderBy(UserActivities.timestamp, SortOrder.DESC)
            .map { toUserActivity(it) }
    }

    fun findByActivityType(activityType: ActivityType, limit: Int = 100): List<UserActivity> = transaction {
        UserActivities.select { UserActivities.activityType eq activityType.name }
            .orderBy(UserActivities.timestamp, SortOrder.DESC)
            .limit(limit)
            .map { toUserActivity(it) }
    }

    fun deleteByUserId(userId: String): Int = transaction {
        UserActivities.deleteWhere { UserActivities.userId eq userId }
    }

    fun deleteOlderThan(cutoffDate: Instant): Int = transaction {
        UserActivities.deleteWhere { timestamp less cutoffDate.toJavaInstant() }
    }

    private fun toUserActivity(row: ResultRow): UserActivity {
        return UserActivity(
            id = row[UserActivities.id],
            userId = row[UserActivities.userId],
            sessionId = row[UserActivities.sessionId],
            activityType = ActivityType.valueOf(row[UserActivities.activityType]),
            timestamp = row[UserActivities.timestamp].toKotlinInstant(),
            duration = row[UserActivities.duration],
            details = row[UserActivities.details]
        )
    }
}
