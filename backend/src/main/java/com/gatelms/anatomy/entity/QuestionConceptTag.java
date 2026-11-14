package com.gatelms.anatomy.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class QuestionConceptTagId implements Serializable {
    @Column(name = "question_id", nullable = false)
    private UUID questionId = UUID.randomUUID();

    @Column(name = "concept_tag_slug", length = 120, nullable = false)
    private String conceptTagSlug = "";

    public QuestionConceptTagId() {}

    public QuestionConceptTagId(UUID questionId, String conceptTagSlug) {
        this.questionId = questionId != null ? questionId : UUID.randomUUID();
        this.conceptTagSlug = conceptTagSlug != null ? conceptTagSlug : "";
    }

    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }

    public String getConceptTagSlug() { return conceptTagSlug; }
    public void setConceptTagSlug(String conceptTagSlug) { this.conceptTagSlug = conceptTagSlug; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuestionConceptTagId that = (QuestionConceptTagId) o;
        return Objects.equals(questionId, that.questionId) && Objects.equals(conceptTagSlug, that.conceptTagSlug);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionId, conceptTagSlug);
    }
}

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
