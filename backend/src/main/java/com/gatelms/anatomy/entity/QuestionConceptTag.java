package com.gatelms.anatomy.entity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "question_concept_tags")
public class QuestionConceptTag {
    @EmbeddedId
    private QuestionConceptTagId id = new QuestionConceptTagId();

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 20, nullable = false)
    private ConceptTagSource source = ConceptTagSource.ANATOMY;

    @Column(name = "confidence", nullable = false)
    private Float confidence = 1.0f;

    @Column(name = "tagged_at", nullable = false, updatable = false)
    private Instant taggedAt = Instant.now();

    public QuestionConceptTag() {}

    public QuestionConceptTag(QuestionConceptTagId id, ConceptTagSource source, Float confidence) {
        this(id, source, confidence, Instant.now());
    }

    public QuestionConceptTag(QuestionConceptTagId id, ConceptTagSource source, Float confidence, Instant taggedAt) {
        this.id = id != null ? id : new QuestionConceptTagId();
        this.source = source != null ? source : ConceptTagSource.ANATOMY;
        this.confidence = confidence != null ? confidence : 1.0f;
        this.taggedAt = taggedAt != null ? taggedAt : Instant.now();
    }

    public QuestionConceptTagId getId() { return id; }
    public void setId(QuestionConceptTagId id) { this.id = id; }

    public ConceptTagSource getSource() { return source; }
    public void setSource(ConceptTagSource source) { this.source = source; }

    public Float getConfidence() { return confidence; }
    public void setConfidence(Float confidence) { this.confidence = confidence; }

    public Instant getTaggedAt() { return taggedAt; }
    public void setTaggedAt(Instant taggedAt) { this.taggedAt = taggedAt; }
}
