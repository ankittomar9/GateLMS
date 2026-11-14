package com.gatelms.anatomy.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "retrieval_cards")
public class RetrievalCard {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_unit_id", nullable = false)
    private ConceptUnit conceptUnit;

    @Column(name = "prompt", columnDefinition = "TEXT", nullable = false)
    private String prompt = "";

    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer = "";

    @Column(name = "card_order", nullable = false)
    private Integer cardOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RetrievalCardStatus status = RetrievalCardStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RetrievalCard() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ConceptUnit getConceptUnit() { return conceptUnit; }
    public void setConceptUnit(ConceptUnit conceptUnit) { this.conceptUnit = conceptUnit; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public Integer getCardOrder() { return cardOrder; }
    public void setCardOrder(Integer cardOrder) { this.cardOrder = cardOrder; }

    public RetrievalCardStatus getStatus() { return status; }
    public void setStatus(RetrievalCardStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
