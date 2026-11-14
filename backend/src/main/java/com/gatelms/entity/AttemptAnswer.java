package com.gatelms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "attempt_answers",
    uniqueConstraints = {@UniqueConstraint(name = "uq_attempt_question", columnNames = {"attempt_id", "question_id"})}
)
public class AttemptAnswer {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private MockAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "selected_options", length = 50)
    private String selectedOptions;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "numeric_answer")
    private Double numericAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 50)
    private AttemptAnswerState state = AttemptAnswerState.NOT_VISITED;

    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds = 0;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "marks_awarded")
    private Double marksAwarded = 0.0;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AttemptAnswer() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public MockAttempt getAttempt() { return attempt; }
    public void setAttempt(MockAttempt attempt) { this.attempt = attempt; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public String getSelectedOptions() { return selectedOptions; }
    public void setSelectedOptions(String selectedOptions) { this.selectedOptions = selectedOptions; }

    public Double getNumericAnswer() { return numericAnswer; }
    public void setNumericAnswer(Double numericAnswer) { this.numericAnswer = numericAnswer; }

    public AttemptAnswerState getState() { return state; }
    public void setState(AttemptAnswerState state) { this.state = state; }

    public Integer getTimeSpentSeconds() { return timeSpentSeconds; }
    public void setTimeSpentSeconds(Integer timeSpentSeconds) { this.timeSpentSeconds = timeSpentSeconds; }

    public Double getMarksAwarded() { return marksAwarded; }
    public void setMarksAwarded(Double marksAwarded) { this.marksAwarded = marksAwarded; }

    public Boolean getIsCorrect() { return isCorrect; }
    public void setIsCorrect(Boolean isCorrect) { this.isCorrect = isCorrect; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
