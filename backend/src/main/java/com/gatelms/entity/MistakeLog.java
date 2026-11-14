package com.gatelms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mistake_logs")
public class MistakeLog {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_category", length = 30)
    private MistakeErrorCategory errorCategory;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 1;

    @Column(name = "first_failed_at", nullable = false, updatable = false)
    private Instant firstFailedAt = Instant.now();

    @Column(name = "last_failed_at", nullable = false)
    private Instant lastFailedAt = Instant.now();

    @Column(name = "next_review_due")
    private Instant nextReviewDue;

    public MistakeLog() {}

    public MistakeLog(UUID id, UUID userId, Question question, MistakeErrorCategory errorCategory, String notes, Integer attemptCount, Instant firstFailedAt, Instant lastFailedAt, Instant nextReviewDue) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        this.question = question;
        this.errorCategory = errorCategory;
        this.notes = notes;
        this.attemptCount = attemptCount != null ? attemptCount : 1;
        this.firstFailedAt = firstFailedAt != null ? firstFailedAt : Instant.now();
        this.lastFailedAt = lastFailedAt != null ? lastFailedAt : Instant.now();
        this.nextReviewDue = nextReviewDue;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public MistakeErrorCategory getErrorCategory() { return errorCategory; }
    public void setErrorCategory(MistakeErrorCategory errorCategory) { this.errorCategory = errorCategory; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getAttemptCount() { return attemptCount; }
    public void setAttemptCount(Integer attemptCount) { this.attemptCount = attemptCount; }

    public Instant getFirstFailedAt() { return firstFailedAt; }
    public void setFirstFailedAt(Instant firstFailedAt) { this.firstFailedAt = firstFailedAt; }

    public Instant getLastFailedAt() { return lastFailedAt; }
    public void setLastFailedAt(Instant lastFailedAt) { this.lastFailedAt = lastFailedAt; }

    public Instant getNextReviewDue() { return nextReviewDue; }
    public void setNextReviewDue(Instant nextReviewDue) { this.nextReviewDue = nextReviewDue; }
}
