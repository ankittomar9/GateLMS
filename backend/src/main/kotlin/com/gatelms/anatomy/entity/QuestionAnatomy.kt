package com.gatelms.anatomy.entity

import com.gatelms.entity.Question
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "question_anatomy")
class QuestionAnatomy(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    var id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    var question: Question? = null,

    @Column(name = "tested_concept", columnDefinition = "TEXT", nullable = false)
    var testedConcept: String = "",

    @Column(name = "invariant_rule", columnDefinition = "TEXT")
    var invariantRule: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trap_analysis", columnDefinition = "jsonb", nullable = false)
    var trapAnalysisJson: String = "[]",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prerequisites", columnDefinition = "jsonb", nullable = false)
    var prerequisitesJson: String = "[]",

    @Column(name = "similar_pattern_hint", columnDefinition = "TEXT")
    var similarPatternHint: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grounding_refs", columnDefinition = "jsonb", nullable = false)
    var groundingRefsJson: String = "[]",

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    var status: AnatomyStatus = AnatomyStatus.DRAFT,

    @Column(name = "parse_error", columnDefinition = "TEXT")
    var parseError: String? = null,

    @Column(name = "generator_model", length = 100)
    var generatorModel: String? = null,

    @Column(name = "generated_at", nullable = false, updatable = false)
    var generatedAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at")
    var updatedAt: Instant? = null,

    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0
)
