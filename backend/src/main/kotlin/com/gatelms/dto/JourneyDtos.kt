package com.gatelms.dto

import com.gatelms.anatomy.entity.TopicPrimerStatus
import java.time.Instant
import java.util.UUID

data class JourneyOverviewSubjectDto(
    val subjectId: String,
    val name: String,
    val cluster: String?,
    val priorityTier: Int,
    val marksWeight: Double,
    val standardBookRef: String?,
    val questionsVisible: Long,
    val questionsSolved: Long,
    val topicsTotal: Int,
    val topicsTouched: Int,
    val topicsCompleted: Int,
    val readinessPct: Double
)

data class JourneyTopicSummaryDto(
    val topicId: String,
    val name: String,
    val sequence: Int,
    val primerStatus: String?,
    val questionsVisible: Long,
    val questionsSolved: Long,
    val masteryLevel: Int,
    val completed: Boolean
)

data class TopicPrimerDto(
    val id: UUID,
    val topicId: String,
    val motivation: String,
    val coreIdea: String,
    val invariant: String?,
    val workedExample: String,
    val traps: String?,
    val status: String,
    val parseError: String?,
    val generatorModel: String?,
    val generatedAt: Instant
)

data class TopicPrimerResponseDto(
    val primer: TopicPrimerDto?,
    val conceptSlugs: List<String>,
    val bookChapterRef: String?,
    val subjectStandardBookRef: String?
)

data class JourneyContinueDto(
    val topicId: String?,
    val subjectId: String?,
    val questionId: UUID?
)

data class CurationPrimerUpdateRequest(
    val status: TopicPrimerStatus? = null,
    val motivation: String? = null,
    val coreIdea: String? = null,
    val invariant: String? = null,
    val workedExample: String? = null,
    val traps: String? = null
)

data class NotebookEntryDto(
    val questionId: UUID,
    val year: Int?,
    val source: String?,
    val statementSnippet: String,
    val solvedAt: Instant?,
    val starred: Boolean,
    val anatomyAvailable: Boolean
)

data class TopicNotebookGroupDto(
    val topicId: String,
    val topicName: String,
    val sequence: Int,
    val entries: List<NotebookEntryDto>
)

data class SubjectNotebookDto(
    val subjectId: String,
    val subjectName: String,
    val topics: List<TopicNotebookGroupDto>,
    val totalEntries: Int
)

data class SimilarQuestionsResponseDto(
    val questions: List<QuestionSummaryDto>,
    val similarityBasis: String
)

