package com.gatelms.anatomy.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class RetrievalCardStateId(
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "card_id", nullable = false)
    var cardId: UUID = UUID.randomUUID()
) : Serializable

@Entity
@Table(name = "retrieval_card_state")
class RetrievalCardState(
    @EmbeddedId
    var id: RetrievalCardStateId = RetrievalCardStateId(),

    @Column(name = "due_at", nullable = false)
    var dueAt: Instant = Instant.now(),

    @Column(name = "interval_days", nullable = false)
    var intervalDays: Float = 0f,

    @Column(name = "ease", nullable = false)
    var ease: Float = 2.5f,

    @Column(name = "consecutive_correct", nullable = false)
    var consecutiveCorrect: Int = 0,

    @Column(name = "last_reviewed_at")
    var lastReviewedAt: Instant? = null
)
