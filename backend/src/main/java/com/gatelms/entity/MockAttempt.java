package com.gatelms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mock_attempts")
public class MockAttempt {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_id", nullable = false)
    private MockExam mockExam;

    @Column(name = "started_at")
    private Instant startedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "total_score")
    private Double totalScore;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "positive_score")
    private Double positiveScore;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "negative_score")
    private Double negativeScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private MockAttemptStatus status = MockAttemptStatus.IN_PROGRESS;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_ids_json")
    private String questionIdsJson;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "finished_reason", length = 20)
    private FinishedReason finishedReason;

    public MockAttempt() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public MockExam getMockExam() { return mockExam; }
    public void setMockExam(MockExam mockExam) { this.mockExam = mockExam; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }

    public Double getPositiveScore() { return positiveScore; }
    public void setPositiveScore(Double positiveScore) { this.positiveScore = positiveScore; }

    public Double getNegativeScore() { return negativeScore; }
    public void setNegativeScore(Double negativeScore) { this.negativeScore = negativeScore; }

    public MockAttemptStatus getStatus() { return status; }
    public void setStatus(MockAttemptStatus status) { this.status = status; }

    public String getQuestionIdsJson() { return questionIdsJson; }
    public void setQuestionIdsJson(String questionIdsJson) { this.questionIdsJson = questionIdsJson; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public FinishedReason getFinishedReason() { return finishedReason; }
    public void setFinishedReason(FinishedReason finishedReason) { this.finishedReason = finishedReason; }
}
