package com.gatelms.dto

import java.time.Instant
import java.time.LocalDate

data class ReadinessResponse(
    val readiness: Int,
    val perSubject: List<SubjectReadinessDto>,
    val computedAt: Instant = Instant.now()
)

data class SubjectReadinessDto(
    val subjectId: String,
    val name: String,
    val weight: Double,
    val m: Double,
    val a: Double,
    val c: Double,
    val readiness: Double
)

data class HeatmapResponse(
    val subjects: List<SubjectHeatmapDto>
)

data class SubjectHeatmapDto(
    val subjectId: String,
    val name: String,
    val topics: List<TopicHeatmapDto>
)

data class TopicHeatmapDto(
    val topicId: String,
    val name: String,
    val mastery: Int,
    val due: Boolean,
    val answered: Int,
    val correct: Int
)

data class TrendsResponse(
    val days: List<DayRollupDto>,
    val rolling7: Rolling7Stats
)

data class DayRollupDto(
    val date: LocalDate,
    val questionsAnswered: Int,
    val correct: Int,
    val minutesPracticed: Int,
    val minutesMocked: Int
)

data class Rolling7Stats(
    val accuracy: Double,
    val volume: Int
)

data class ExamCountdownDto(
    val examDate: String,
    val daysRemaining: Long,
    val weeksRemaining: Long,
    val source: String = "config"
)
