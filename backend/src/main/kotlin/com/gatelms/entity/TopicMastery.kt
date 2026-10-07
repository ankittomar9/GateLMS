package com.gatelms.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class TopicMasteryId(
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "topic_id", length = 100, nullable = false)
    var topicId: String = ""
) : Serializable

@Entity
@Table(name = "topic_mastery")
class TopicMastery(
    @EmbeddedId
    var id: TopicMasteryId = TopicMasteryId(),

    @Column(name = "mastery_level", nullable = false)
    var masteryLevel: Int = 0,

    @Column(name = "last_revised_at")
    var lastRevisedAt: Instant? = null,

    @Column(name = "next_revision_due")
    var nextRevisionDue: Instant? = null
)
