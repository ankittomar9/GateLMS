package com.gatelms.dto

import com.gatelms.entity.PlanItemStatus
import com.gatelms.entity.PlanStrategy
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class GeneratePlanRequest(
    val title: String? = null,
    val weeks: Int? = null,
    val strategy: PlanStrategy = PlanStrategy.WEIGHTED,
    val weeklyTargetQuestions: Int? = 150,
    val weeklyTargetMinutes: Int? = 600,
    val startDate: LocalDate? = null
)

data class PlanItemDto(
    val id: UUID,
    val planId: UUID,
    val planDate: LocalDate,
    val subjectId: String?,
    val subjectName: String?,
    val subjectCode: String?,
    val topicId: String?,
    val topicName: String?,
    val targetQuestions: Int,
    val targetMinutes: Int,
    val doneQuestions: Int,
    val doneMinutes: Int,
    val status: PlanItemStatus
)

data class StudyPlanSummaryDto(
    val id: UUID,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val weeks: Int,
    val weeklyTargetQuestions: Int,
    val weeklyTargetMinutes: Int,
    val strategy: PlanStrategy,
    val totalQuestionsTarget: Int,
    val totalQuestionsDone: Int,
    val totalMinutesTarget: Int,
    val totalMinutesDone: Int,
    val progressPercentage: Double,
    val itemsCount: Int,
    val openItemsCount: Int,
    val doneItemsCount: Int,
    val skippedItemsCount: Int,
    val createdAt: Instant
)

data class StudyPlanDetailDto(
    val id: UUID,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val weeks: Int,
    val weeklyTargetQuestions: Int,
    val weeklyTargetMinutes: Int,
    val strategy: PlanStrategy,
    val totalQuestionsTarget: Int,
    val totalQuestionsDone: Int,
    val totalMinutesTarget: Int,
    val totalMinutesDone: Int,
    val progressPercentage: Double,
    val generatedReasoning: Map<String, Any?>?,
    val createdAt: Instant,
    val items: List<PlanItemDto>,
    val itemsByDate: Map<LocalDate, List<PlanItemDto>>
)

data class TodayPlanResponse(
    val items: List<PlanItemDto>,
    val doneQuestions: Int,
    val targetQuestions: Int,
    val minutesDone: Int,
    val minutesTarget: Int
)

data class UpdatePlanItemStatusRequest(
    val status: PlanItemStatus
)
