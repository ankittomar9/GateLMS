package com.gatelms.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "mock_attempts")
class MockAttempt(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_id", nullable = false)
    var mockExam: MockExam? = null,

    @Column(name = "started_at")
    var startedAt: Instant = Instant.now(),

    @Column(name = "submitted_at")
    var submittedAt: Instant? = null,

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "total_score")
    var totalScore: Double? = null,

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "positive_score")
    var positiveScore: Double? = null,

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "negative_score")
    var negativeScore: Double? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    var status: MockAttemptStatus = MockAttemptStatus.IN_PROGRESS,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_ids_json")
    var questionIdsJson: String? = null,

    @Column(name = "duration_seconds")
    var durationSeconds: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "finished_reason", length = 20)
    var finishedReason: FinishedReason? = null
)
