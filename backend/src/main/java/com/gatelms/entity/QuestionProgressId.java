package com.gatelms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class QuestionProgressId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "question_id")
    private UUID questionId;

    public QuestionProgressId() {}

    public QuestionProgressId(UUID userId, UUID questionId) {
        this.userId = userId;
        this.questionId = questionId;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getQuestionId() { return questionId; }
    public void setQuestionId(UUID questionId) { this.questionId = questionId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuestionProgressId that = (QuestionProgressId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(questionId, that.questionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, questionId);
    }
}
