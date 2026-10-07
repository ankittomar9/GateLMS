package com.gatelms.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "attempt_answers",
    uniqueConstraints = [UniqueConstraint(name = "uq_attempt_question", columnNames = ["attempt_id", "question_id"])]
)
class AttemptAnswer(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    var attempt: MockAttempt? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    var question: Question? = null,

    @Column(name = "selected_options", length = 50)
    var selectedOptions: String? = null,

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "numeric_answer")
    var numericAnswer: Double? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 50)
    var state: AttemptAnswerState = AttemptAnswerState.NOT_VISITED,

    @Column(name = "time_spent_seconds")
    var timeSpentSeconds: Int = 0,

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "marks_awarded")
    var marksAwarded: Double = 0.0,

    @Column(name = "is_correct")
    var isCorrect: Boolean? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
