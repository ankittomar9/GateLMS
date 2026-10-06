package com.gatelms.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "questions")
class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    var id: UUID? = null,

    @Column(name = "external_id", length = 100, unique = true)
    var externalId: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    var subject: Subject? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    var topic: Topic? = null,

    @Column(name = "year")
    var year: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    var type: QuestionType = QuestionType.MCQ,

    @Column(name = "marks", nullable = false)
    var marks: Int = 1,

    @Column(name = "statement", columnDefinition = "TEXT", nullable = false)
    var statement: String = "",

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "jsonb")
    var optionsJson: String? = null,

    @Column(name = "correct_options", length = 50)
    var correctOptions: String? = null,

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "numeric_min")
    var numericMin: Double? = null,

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "numeric_max")
    var numericMax: Double? = null,

    @Column(name = "explanation", columnDefinition = "TEXT")
    var explanation: String? = null,

    @Column(name = "gate_overflow_url", length = 500)
    var gateOverflowUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "pass1_status", length = 20, nullable = false)
    var pass1Status: Pass1Status = Pass1Status.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(name = "pass2_status", length = 20, nullable = false)
    var pass2Status: Pass2Status = Pass2Status.PENDING,

    @Column(name = "is_bookmarked", nullable = false)
    var isBookmarked: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = Instant.now(),

    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0
)
