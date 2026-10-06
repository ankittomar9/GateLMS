package com.gatelms.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "topics",
    uniqueConstraints = [UniqueConstraint(name = "uq_subject_topic", columnNames = ["subject_id", "slug"])]
)
class Topic(
    @Id
    @Column(name = "id", length = 100, nullable = false)
    var id: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    var subject: Subject? = null,

    @Column(name = "name", length = 200, nullable = false)
    var name: String = "",

    @Column(name = "slug", length = 150, nullable = false)
    var slug: String = "",

    @Column(name = "book_chapter_ref", length = 255)
    var bookChapterRef: String? = null,

    @Column(name = "mastery_level", nullable = false)
    var masteryLevel: Int = 0,

    @Column(name = "last_revised_at")
    var lastRevisedAt: Instant? = null,

    @Column(name = "next_revision_due")
    var nextRevisionDue: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = Instant.now(),

    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0
)
