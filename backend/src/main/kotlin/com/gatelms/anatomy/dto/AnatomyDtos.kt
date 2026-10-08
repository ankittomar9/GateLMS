package com.gatelms.anatomy.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.gatelms.anatomy.entity.AnatomyStatus
import com.gatelms.anatomy.entity.ConceptUnitStatus
import com.gatelms.anatomy.entity.RetrievalCardStatus
import java.time.Instant
import java.util.UUID

data class TrapAnalysisItem(
    @JsonProperty("option")
    val option: String = "",

    @JsonProperty("verdict")
    val verdict: String = "INCORRECT",

    @JsonProperty("why_plausible")
    val whyPlausible: String = "",

    @JsonProperty("misconception")
    val misconception: String = ""
)

data class PrerequisiteItem(
    @JsonProperty("concept_slug")
    val conceptSlug: String = "",

    @JsonProperty("concept_label")
    val conceptLabel: String = "",

    @JsonProperty("topic_slug")
    val topicSlug: String? = null,

    @JsonProperty("must_read")
    val mustRead: Boolean = true
)

data class AnatomyJsonPayload(
    @JsonProperty("tested_concept")
    val testedConcept: String = "",

    @JsonProperty("invariant_rule")
    val invariantRule: String? = null,

    @JsonProperty("trap_analysis")
    val trapAnalysis: List<TrapAnalysisItem> = emptyList(),

    @JsonProperty("prerequisites")
    val prerequisites: List<PrerequisiteItem> = emptyList(),

    @JsonProperty("concept_tags")
    val conceptTags: List<String> = emptyList(),

    @JsonProperty("similar_pattern_hint")
    val similarPatternHint: String? = null
)

data class QuestionAnatomyDto(
    val id: UUID,
    val questionId: UUID,
    val testedConcept: String,
    val invariantRule: String?,
    val trapAnalysis: List<TrapAnalysisItem>,
    val prerequisites: List<PrerequisiteItem>,
    val similarPatternHint: String?,
    val status: AnatomyStatus,
    val parseError: String?,
    val generatorModel: String?,
    val generatedAt: Instant,
    val updatedAt: Instant?
)

data class ConceptUnitDto(
    val id: UUID,
    val conceptTagSlug: String,
    val title: String,
    val motivation: String,
    val coreIdea: String,
    val invariant: String?,
    val workedExample: String,
    val traps: String?,
    val deepLinkRef: String?,
    val status: ConceptUnitStatus,
    val parseError: String?,
    val generatorModel: String?,
    val generatedAt: Instant,
    val cards: List<RetrievalCardDto> = emptyList()
)

data class RetrievalCardDto(
    val id: UUID,
    val prompt: String,
    val answer: String,
    val cardOrder: Int,
    val status: RetrievalCardStatus
)

data class ConceptSummaryDto(
    val slug: String,
    val label: String,
    val topicId: String,
    val questionCount: Long
)

data class CurationAnatomyUpdateRequest(
    val status: AnatomyStatus? = null,
    val testedConcept: String? = null,
    val invariantRule: String? = null,
    val similarPatternHint: String? = null
)

// ---- WP2: Retrieval card scheduling contract (SPEC §6.3) ----

data class DueCardDto(
    val cardId: UUID,
    val prompt: String,
    val answer: String,
    val conceptSlug: String,
    val conceptLabel: String,
    val unitTitle: String,
    val status: RetrievalCardStatus
)

data class DueCardsResponse(
    val dueCount: Int,
    val cards: List<DueCardDto>
)

data class CardReviewRequest(
    val grade: Int
)

data class CardReviewResponse(
    val nextDueAt: Instant,
    val intervalDays: Float,
    val ease: Float
)
