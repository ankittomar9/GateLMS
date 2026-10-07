package com.gatelms.dto

import java.time.Instant
import java.util.UUID

data class BlueprintSection(
    val subjectId: String = "",
    val mcq: Int = 0,
    val msq: Int = 0,
    val nat: Int = 0,
    val marks: Int? = null
)

data class BlueprintQuestionPool(
    val curationStatus: List<String>? = listOf("ACCEPTED", "UNREVIEWED"),
    val difficulty: List<String>? = null
)

data class MockBlueprint(
    val title: String? = null,
    val sections: List<BlueprintSection> = emptyList(),
    val questionPool: BlueprintQuestionPool? = null,
    val shuffle: Boolean = true
)

data class CreateMockTemplateRequest(
    val title: String,
    val durationMinutes: Int = 180,
    val totalMarks: Double? = null,
    val sections: List<BlueprintSection> = emptyList(),
    val questionPool: BlueprintQuestionPool? = null,
    val shuffle: Boolean = true
)

data class MockTemplateSummaryDto(
    val id: UUID,
    val title: String,
    val durationMinutes: Int,
    val totalMarks: Double,
    val totalQuestions: Int,
    val mode: String,
    val createdAt: Instant,
    val sections: List<BlueprintSection>,
    val bestScore: Double? = null,
    val attemptCount: Int = 0
)

data class MockAttemptSectionDto(
    val subjectId: String,
    val name: String,
    val questionIds: List<UUID>
)

data class StartMockAttemptResponse(
    val attemptId: UUID,
    val mockId: UUID,
    val mockTitle: String,
    val durationSeconds: Int,
    val sections: List<MockAttemptSectionDto>,
    val questions: List<PracticeQuestionDto>,
    val startedAt: Instant,
    val serverTime: Instant,
    val shortfallWarnings: List<String> = emptyList()
)

data class UpsertAnswerRequest(
    val selectedOptions: String? = null,
    val numericAnswer: Double? = null,
    val state: String? = null,
    val timeSpentSeconds: Int? = null
)

data class UpsertAnswerResponse(
    val saved: Boolean = true,
    val state: String
)

data class SubmitMockAttemptRequest(
    val timeSpentSeconds: Int? = null
)

data class SubmitMockAttemptResponse(
    val attemptId: UUID,
    val status: String,
    val finishedReason: String,
    val positiveScore: Double,
    val negativeScore: Double,
    val totalScore: Double,
    val maxMarks: Double,
    val correctCount: Int,
    val wrongCount: Int,
    val unattemptedCount: Int,
    val totalQuestions: Int
)

data class MockQuestionReviewDto(
    val questionId: UUID,
    val externalId: String,
    val type: String,
    val marks: Int,
    val statement: String,
    val optionsJson: String?,
    val subjectId: String?,
    val subjectName: String?,
    val topicId: String?,
    val yourAnswer: String?,
    val numericAnswer: Double?,
    val correctAnswer: String?,
    val numericMin: Double?,
    val numericMax: Double?,
    val isCorrect: Boolean?,
    val marksAwarded: Double,
    val state: String,
    val timeSpentSeconds: Int,
    val explanation: String?
)

data class MockSectionScoreDto(
    val subjectId: String,
    val subjectName: String,
    val totalQuestions: Int,
    val attemptedCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val unattemptedCount: Int,
    val marksAwarded: Double,
    val maxMarks: Double,
    val accuracy: Double,
    val timeSpentSeconds: Int
)

data class MockResultResponse(
    val attemptId: UUID,
    val mockId: UUID,
    val mockTitle: String,
    val startedAt: Instant,
    val submittedAt: Instant?,
    val durationSeconds: Int,
    val finishedReason: String,
    val totalScore: Double,
    val positiveScore: Double,
    val negativeScore: Double,
    val maxMarks: Double,
    val timeSpentSeconds: Int,
    val questions: List<MockQuestionReviewDto>,
    val sectionScores: List<MockSectionScoreDto>
)

data class MockAttemptHistoryDto(
    val attemptId: UUID,
    val startedAt: Instant,
    val submittedAt: Instant?,
    val durationSeconds: Int?,
    val totalScore: Double?,
    val positiveScore: Double?,
    val negativeScore: Double?,
    val status: String,
    val finishedReason: String?
)

data class MockTrendDto(
    val attemptId: UUID,
    val mockId: UUID,
    val mockTitle: String,
    val date: Instant,
    val totalScore: Double,
    val maxScore: Double,
    val percentage: Double,
    val percentileLike: Double? = null
)
