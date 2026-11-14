package com.gatelms.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class DailyRollupId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate = LocalDate.now();

    public DailyRollupId() {}

    public DailyRollupId(UUID userId, LocalDate activityDate) {
        this.userId = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        this.activityDate = activityDate != null ? activityDate : LocalDate.now();
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate activityDate) { this.activityDate = activityDate; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyRollupId that = (DailyRollupId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(activityDate, that.activityDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, activityDate);
    }
}

@Entity
@Table(name = "daily_rollups")
public class DailyRollup {
    @EmbeddedId
    private DailyRollupId id = new DailyRollupId();

    @Column(name = "questions_answered", nullable = false)
    private Integer questionsAnswered = 0;

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers = 0;

    @Column(name = "minutes_practiced", nullable = false)
    private Integer minutesPracticed = 0;

    @Column(name = "minutes_mocked", nullable = false)
    private Integer minutesMocked = 0;

    @Column(name = "mock_attempts", nullable = false)
    private Integer mockAttempts = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public DailyRollup() {}

    public DailyRollup(UUID userId, LocalDate activityDate, Integer questionsAnswered, Integer correctAnswers, Integer minutesPracticed, Integer minutesMocked, Integer mockAttempts, Instant updatedAt) {
        this.id = new DailyRollupId(userId, activityDate);
        this.questionsAnswered = questionsAnswered != null ? questionsAnswered : 0;
        this.correctAnswers = correctAnswers != null ? correctAnswers : 0;
        this.minutesPracticed = minutesPracticed != null ? minutesPracticed : 0;
        this.minutesMocked = minutesMocked != null ? minutesMocked : 0;
        this.mockAttempts = mockAttempts != null ? mockAttempts : 0;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public DailyRollupId getId() { return id; }
    public void setId(DailyRollupId id) { this.id = id; }

    public LocalDate getActivityDate() { return id.getActivityDate(); }
    public UUID getUserId() { return id.getUserId(); }

    public Integer getQuestionsAnswered() { return questionsAnswered; }
    public void setQuestionsAnswered(Integer questionsAnswered) { this.questionsAnswered = questionsAnswered; }

    public Integer getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(Integer correctAnswers) { this.correctAnswers = correctAnswers; }

    public Integer getMinutesPracticed() { return minutesPracticed; }
    public void setMinutesPracticed(Integer minutesPracticed) { this.minutesPracticed = minutesPracticed; }

    public Integer getMinutesMocked() { return minutesMocked; }
    public void setMinutesMocked(Integer minutesMocked) { this.minutesMocked = minutesMocked; }

    public Integer getMockAttempts() { return mockAttempts; }
    public void setMockAttempts(Integer mockAttempts) { this.mockAttempts = mockAttempts; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
