package com.gatelms.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class QuestionProgressId(
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "question_id", nullable = false)
    var questionId: UUID = UUID.randomUUID()
) : Serializable

@Entity
@Table(name = "question_progress")
class QuestionProgress(
    @EmbeddedId
    var id: QuestionProgressId = QuestionProgressId(),

    @Enumerated(EnumType.STRING)
    @Column(name = "pass1_status", length = 20, nullable = false)
    var pass1Status: Pass1Status = Pass1Status.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(name = "pass2_status", length = 20, nullable = false)
    var pass2Status: Pass2Status = Pass2Status.PENDING,

    @Column(name = "is_bookmarked", nullable = false)
    var isBookmarked: Boolean = false,

    @Column(name = "bookmarked_at")
    var bookmarkedAt: Instant? = null,

    @Column(name = "last_attempted_at")
    var lastAttemptedAt: Instant? = null,

    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0
)
