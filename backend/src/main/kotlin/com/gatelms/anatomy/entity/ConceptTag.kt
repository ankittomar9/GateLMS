package com.gatelms.anatomy.entity

import com.gatelms.entity.Topic
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "concept_tags")
class ConceptTag(
    @Id
    @Column(name = "slug", length = 120, nullable = false)
    var slug: String = "",

    @Column(name = "label", length = 200, nullable = false)
    var label: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    var topic: Topic? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
