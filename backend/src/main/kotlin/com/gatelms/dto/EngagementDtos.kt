package com.gatelms.dto

import com.gatelms.entity.NotificationType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class DailyGoalProgressDto(
    val target: Int,
    val done: Int,
    val met: Boolean
)

data class DueTodayDto(
    val mistakes: Long,
    val topics: Long
)

data class EngagementSummaryResponse(
    val streak: Int,
    val protectedDate: LocalDate? = null,
    val streakAtRisk: Boolean,
    val goal: DailyGoalProgressDto,
    val dueToday: DueTodayDto
)

data class AchievementDto(
    val code: String,
    val title: String,
    val description: String,
    val earnedAt: Instant? = null
)

data class NotificationDto(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val body: String?,
    val link: String?,
    val readAt: Instant?,
    val createdAt: Instant
)

data class UnreadCountResponse(
    val count: Long
)

data class UserGoalDto(
    val dailyTargetQuestions: Int,
    val streakProtection: Boolean,
    val updatedAt: Instant
)

data class UpdateUserGoalRequest(
    val dailyTargetQuestions: Int? = null,
    val streakProtection: Boolean? = null
)

data class MarkAllReadResponse(
    val count: Int
)
