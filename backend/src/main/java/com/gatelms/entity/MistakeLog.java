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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_answer_id")
    private AttemptAnswer attemptAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_category", length = 50, nullable = false)
    private MistakeErrorCategory errorCategory = MistakeErrorCategory.CONCEPTUAL;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "corrective_takeaway", columnDefinition = "text", nullable = false)
    private String correctiveTakeaway = "";

    @Column(name = "logged_at", nullable = false, updatable = false)
    private Instant loggedAt = Instant.now();

    @Column(name = "review_count", nullable = false)
    private int reviewCount = 0;

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Column(name = "resolved", nullable = false)
    private boolean resolved = false;

    public MistakeLog() {}

    public MistakeLog(UUID id, UUID userId, Question question, AttemptAnswer attemptAnswer, MistakeErrorCategory errorCategory, String description, String correctiveTakeaway, Instant loggedAt, int reviewCount, Instant nextReviewAt, boolean resolved) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        this.question = question;
        this.attemptAnswer = attemptAnswer;
        this.errorCategory = errorCategory != null ? errorCategory : MistakeErrorCategory.CONCEPTUAL;
        this.description = description;
        this.correctiveTakeaway = correctiveTakeaway != null ? correctiveTakeaway : "";
        this.loggedAt = loggedAt != null ? loggedAt : Instant.now();
        this.reviewCount = reviewCount;
        this.nextReviewAt = nextReviewAt;
        this.resolved = resolved;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public AttemptAnswer getAttemptAnswer() { return attemptAnswer; }
    public void setAttemptAnswer(AttemptAnswer attemptAnswer) { this.attemptAnswer = attemptAnswer; }

    public MistakeErrorCategory getErrorCategory() { return errorCategory; }
    public void setErrorCategory(MistakeErrorCategory errorCategory) { this.errorCategory = errorCategory; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCorrectiveTakeaway() { return correctiveTakeaway; }
    public void setCorrectiveTakeaway(String correctiveTakeaway) { this.correctiveTakeaway = correctiveTakeaway; }

    public Instant getLoggedAt() { return loggedAt; }
    public void setLoggedAt(Instant loggedAt) { this.loggedAt = loggedAt; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public Instant getNextReviewAt() { return nextReviewAt; }
    public void setNextReviewAt(Instant nextReviewAt) { this.nextReviewAt = nextReviewAt; }

    public boolean isResolved() { return resolved; }
    public boolean getResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}
