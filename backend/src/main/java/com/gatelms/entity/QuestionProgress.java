package com.gatelms.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class QuestionProgressId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Column(name = "question_id", nullable = false)
    private UUID questionId = UUID.randomUUID();

    public QuestionProgressId() {}

    public QuestionProgressId(UUID userId, UUID questionId) {
        this.userId = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        this.questionId = questionId != null ? questionId : UUID.randomUUID();
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

@Entity
@Table(name = "question_progress")
public class QuestionProgress {
    @EmbeddedId
    private QuestionProgressId id = new QuestionProgressId();

    @Enumerated(EnumType.STRING)
    @Column(name = "pass1_status", length = 20, nullable = false)
    private Pass1Status pass1Status = Pass1Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass2_status", length = 20, nullable = false)
    private Pass2Status pass2Status = Pass2Status.PENDING;

    @Column(name = "is_bookmarked", nullable = false)
    private Boolean isBookmarked = false;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    public QuestionProgress() {}

    public QuestionProgress(QuestionProgressId id, Pass1Status pass1Status, Pass2Status pass2Status, Boolean isBookmarked, Instant lastAttemptedAt) {
        this.id = id != null ? id : new QuestionProgressId();
        this.pass1Status = pass1Status != null ? pass1Status : Pass1Status.PENDING;
        this.pass2Status = pass2Status != null ? pass2Status : Pass2Status.PENDING;
        this.isBookmarked = isBookmarked != null ? isBookmarked : false;
        this.lastAttemptedAt = lastAttemptedAt;
    }

    public QuestionProgressId getId() { return id; }
    public void setId(QuestionProgressId id) { this.id = id; }

    public Pass1Status getPass1Status() { return pass1Status; }
    public void setPass1Status(Pass1Status pass1Status) { this.pass1Status = pass1Status; }

    public Pass2Status getPass2Status() { return pass2Status; }
    public void setPass2Status(Pass2Status pass2Status) { this.pass2Status = pass2Status; }

    public Boolean getIsBookmarked() { return isBookmarked; }
    public void setIsBookmarked(Boolean isBookmarked) { this.isBookmarked = isBookmarked; }

    public Instant getLastAttemptedAt() { return lastAttemptedAt; }
    public void setLastAttemptedAt(Instant lastAttemptedAt) { this.lastAttemptedAt = lastAttemptedAt; }
}
