package com.gatelms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "mock_attempts")
public class MockAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private MockExam exam;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", length = 20, nullable = false)
    private ExamMode mode = ExamMode.MOCK;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MockAttemptStatus status = MockAttemptStatus.IN_PROGRESS;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "time_remaining_seconds", nullable = false)
    private Integer timeRemainingSeconds = 10800;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "score")
    private Double score;

    @Column(name = "total_marks")
    private Integer totalMarks;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "questions_order", columnDefinition = "jsonb")
    private String questionsOrder;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AttemptAnswer> answers = new ArrayList<>();

    public MockAttempt() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public MockExam getExam() { return exam; }
    public void setExam(MockExam exam) { this.exam = exam; }

    public ExamMode getMode() { return mode; }
    public void setMode(ExamMode mode) { this.mode = mode; }

    public MockAttemptStatus getStatus() { return status; }
    public void setStatus(MockAttemptStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Integer getTimeRemainingSeconds() { return timeRemainingSeconds; }
    public void setTimeRemainingSeconds(Integer timeRemainingSeconds) { this.timeRemainingSeconds = timeRemainingSeconds; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }

    public Integer getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Integer totalMarks) { this.totalMarks = totalMarks; }

    public String getQuestionsOrder() { return questionsOrder; }
    public void setQuestionsOrder(String questionsOrder) { this.questionsOrder = questionsOrder; }

    public List<AttemptAnswer> getAnswers() { return answers; }
    public void setAnswers(List<AttemptAnswer> answers) { this.answers = answers; }
}
