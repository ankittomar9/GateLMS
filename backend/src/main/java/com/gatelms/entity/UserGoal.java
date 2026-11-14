package com.gatelms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_goals")
public class UserGoal {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Column(name = "daily_target_questions", nullable = false)
    private Integer dailyTargetQuestions = 20;

    @Column(name = "streak_protection", nullable = false)
    private Boolean streakProtection = true;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public UserGoal() {}

    public UserGoal(UUID id, UUID userId, Integer dailyTargetQuestions, Boolean streakProtection, Instant updatedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        this.dailyTargetQuestions = dailyTargetQuestions != null ? dailyTargetQuestions : 20;
        this.streakProtection = streakProtection != null ? streakProtection : true;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Integer getDailyTargetQuestions() { return dailyTargetQuestions; }
    public void setDailyTargetQuestions(Integer dailyTargetQuestions) { this.dailyTargetQuestions = dailyTargetQuestions; }

    public Boolean getStreakProtection() { return streakProtection; }
    public void setStreakProtection(Boolean streakProtection) { this.streakProtection = streakProtection; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
