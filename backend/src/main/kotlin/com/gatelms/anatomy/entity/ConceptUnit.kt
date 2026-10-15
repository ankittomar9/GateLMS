package com.gatelms.anatomy.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "concept_units")
class ConceptUnit(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    var id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_tag_slug", referencedColumnName = "slug", nullable = false, unique = true)
    var conceptTag: ConceptTag? = null,

    @Column(name = "title", length = 300, nullable = false)
    var title: String = "",

    @Column(name = "motivation", columnDefinition = "TEXT", nullable = false)
    var motivation: String = "",

    @Column(name = "core_idea", columnDefinition = "TEXT", nullable = false)
    var coreIdea: String = "",

    @Column(name = "invariant", columnDefinition = "TEXT")
    var invariant: String? = null,

    @Column(name = "worked_example", columnDefinition = "TEXT", nullable = false)
    var workedExample: String = "",

    @Column(name = "worked_example_external_id", length = 120)
    var workedExampleExternalId: String? = null,

    @Column(name = "traps", columnDefinition = "TEXT")
    var traps: String? = null,

    @Column(name = "deep_link_ref", columnDefinition = "TEXT")
    var deepLinkRef: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    var status: ConceptUnitStatus = ConceptUnitStatus.DRAFT,

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
