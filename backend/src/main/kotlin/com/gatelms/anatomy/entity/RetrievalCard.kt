package com.gatelms.anatomy.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "retrieval_cards")
class RetrievalCard(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    var id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_unit_id", nullable = false)
    var conceptUnit: ConceptUnit? = null,

    @Column(name = "prompt", columnDefinition = "TEXT", nullable = false)
    var prompt: String = "",

    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    var answer: String = "",

    @Column(name = "card_order", nullable = false)
    var cardOrder: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    var status: RetrievalCardStatus = RetrievalCardStatus.DRAFT,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
