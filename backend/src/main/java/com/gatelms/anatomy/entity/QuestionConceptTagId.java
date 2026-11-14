package com.gatelms.anatomy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class QuestionConceptTagId implements Serializable {

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "concept_tag_slug", length = 120)
    private String conceptTagSlug;

    public QuestionConceptTagId() {}

    public QuestionConceptTagId(UUID questionId, String conceptTagSlug) {
        this.questionId = questionId;
        this.conceptTagSlug = conceptTagSlug;
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
