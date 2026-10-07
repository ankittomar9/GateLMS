package com.gatelms.dto

import com.gatelms.entity.MistakeErrorCategory
import com.gatelms.entity.QuestionType
import java.time.Instant
import java.util.UUID

data class PracticeSessionCreateRequest(
    val subjectId: String? = null,
    val topicId: String? = null,
    val questionType: QuestionType? = null,
    val marks: Int? = null,
    val curationStatus: String? = null,
    val count: Int = 10,
    val year: Int? = null
)

data class PracticeQuestionDto(
    val id: UUID,
    val externalId: String?,
    val type: QuestionType,
    val marks: Int,
    val statement: String,
    val optionsJson: String?,
    val topicId: String?,
    val subjectId: String?,
    val year: Int?
)

data class PracticeSessionResponse(
    val sessionId: UUID,
    val attemptId: UUID,
    val questions: List<PracticeQuestionDto>,
    val totalQuestions: Int
)

data class PracticeAnswerSubmission(
    val questionId: UUID,
    val selectedOptions: String? = null,
    val numericAnswer: Double? = null,
    val timeSpentSeconds: Int = 0
)

data class PracticeVerdictResponse(
    val correct: Boolean,
    val marksAwarded: Double,
    val correctOptions: String? = null,
    val numericMin: Double? = null,
    val numericMax: Double? = null,
    val explanation: String? = null,
    val topicMastery: Int,
    val mistakeLogPrompt: Boolean
)

data class PracticeQuestionActionRequest(
    val questionId: UUID
)

data class PracticeTopicSummary(
    val topicId: String,
    val name: String,
    val correct: Int,
    val total: Int
)

data class PracticeFinishResponse(
    val correct: Int,
    val wrong: Int,
    val skipped: Int,
    val totalQuestions: Int,
    val timeSpentSeconds: Int,
    val byTopic: List<PracticeTopicSummary>
)

data class MistakeCreateRequest(
    val questionId: UUID,
    val attemptAnswerId: UUID? = null,
    val errorCategory: String,
    val description: String? = null,
    val correctiveTakeaway: String
)

data class MistakeReviewRequest(
    val resolved: Boolean? = null
)

data class MistakeDto(
    val id: UUID,
    val questionId: UUID,
    val questionExternalId: String?,
    val questionStatement: String,
    val attemptAnswerId: UUID?,
    val errorCategory: MistakeErrorCategory,
    val description: String?,
    val correctiveTakeaway: String,
    val loggedAt: Instant,
    val reviewCount: Int,
    val nextReviewAt: Instant?,
    val resolved: Boolean
)

data class TopicMasteryStatsDto(
    val topicId: String,
    val topicName: String,
    val subjectId: String,
    val masteryLevel: Int,
    val dueReviewsCount: Long
)
