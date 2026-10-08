package com.gatelms.anatomy.entity

import com.gatelms.entity.Topic
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "topic_primers")
class TopicPrimer(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    var id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", referencedColumnName = "id", nullable = false, unique = true)
    var topic: Topic? = null,

    @Column(name = "motivation", columnDefinition = "TEXT", nullable = false)
    var motivation: String = "",

    @Column(name = "core_idea", columnDefinition = "TEXT", nullable = false)
    var coreIdea: String = "",

    @Column(name = "invariant", columnDefinition = "TEXT")
    var invariant: String? = null,

    @Column(name = "worked_example", columnDefinition = "TEXT", nullable = false)
    var workedExample: String = "",

    @Column(name = "traps", columnDefinition = "TEXT")
    var traps: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    var status: TopicPrimerStatus = TopicPrimerStatus.DRAFT,

    @Column(name = "parse_error", columnDefinition = "TEXT")
    var parseError: String? = null,

    @Column(name = "generator_model", length = 100)
    var generatorModel: String? = null,

    @Column(name = "generated_at", nullable = false, updatable = false)
    var generatedAt: Instant = Instant.now(),

    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0
)
