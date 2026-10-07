package com.gatelms.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "mistake_logs")
class MistakeLog(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    var question: Question? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_answer_id")
    var attemptAnswer: AttemptAnswer? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "error_category", length = 50, nullable = false)
    var errorCategory: MistakeErrorCategory = MistakeErrorCategory.CONCEPTUAL,

    @Column(name = "description", columnDefinition = "text")
    var description: String? = null,

    @Column(name = "corrective_takeaway", columnDefinition = "text", nullable = false)
    var correctiveTakeaway: String = "",

    @Column(name = "logged_at", nullable = false, updatable = false)
    var loggedAt: Instant = Instant.now(),

    @Column(name = "review_count", nullable = false)
    var reviewCount: Int = 0,

    @Column(name = "next_review_at")
    var nextReviewAt: Instant? = null,

    @Column(name = "resolved", nullable = false)
    var resolved: Boolean = false
)
