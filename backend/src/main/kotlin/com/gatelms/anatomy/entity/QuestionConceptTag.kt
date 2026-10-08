package com.gatelms.anatomy.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class QuestionConceptTagId(
    @Column(name = "question_id", nullable = false)
    var questionId: UUID = UUID.randomUUID(),

    @Column(name = "concept_tag_slug", length = 120, nullable = false)
    var conceptTagSlug: String = ""
) : Serializable

@Entity
@Table(name = "question_concept_tags")
class QuestionConceptTag(
    @EmbeddedId
    var id: QuestionConceptTagId = QuestionConceptTagId(),

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20, nullable = false)
    var source: ConceptTagSource = ConceptTagSource.ANATOMY,

    @Column(name = "confidence", nullable = false)
    var confidence: Float = 1.0f,

    @Column(name = "tagged_at", nullable = false, updatable = false)
    var taggedAt: Instant = Instant.now()
)
