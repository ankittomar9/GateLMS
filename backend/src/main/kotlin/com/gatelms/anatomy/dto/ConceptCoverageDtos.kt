package com.gatelms.anatomy.dto

import com.gatelms.entity.QuestionType
import java.util.UUID

data class ConceptCoverageDto(
    val slug: String,
    val label: String,
    val mappedQuestionCount: Long,
    val uniqueStatementCount: Long,
    val lessonStatus: String?,
    val coverage: String
)

data class TopicConceptCoverageResponseDto(
    val topicId: String,
    val conceptsTotal: Int,
    val conceptsWithMaterial: Int,
    val conceptsExplained: Int,
    val concepts: List<ConceptCoverageDto>
)

data class UnmappedMappingQuestionDto(
    val id: UUID,
    val externalId: String?,
    val statement: String,
    val type: QuestionType,
    val topicId: String?,
    val topicName: String?,
    val conceptTags: List<String> = emptyList(),
    val lowestConfidence: Float? = null
)

data class CurationQuestionConceptsPatchRequest(
    val tags: List<String>? = null,
    val addTags: List<String> = emptyList(),
    val removeTags: List<String> = emptyList()
)
