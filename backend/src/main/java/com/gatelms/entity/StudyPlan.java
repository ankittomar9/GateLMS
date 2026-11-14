package com.gatelms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "study_plans")
public class StudyPlan {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Column(name = "name", length = 150, nullable = false)
    private String name = "";

    @Column(name = "target_exam_date", nullable = false)
    private LocalDate targetExamDate = LocalDate.now();

    @Column(name = "daily_hours", nullable = false)
    private Double dailyHours = 2.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", length = 30, nullable = false)
    private PlanStrategy strategy = PlanStrategy.WEIGHTED;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public StudyPlan() {}

    public StudyPlan(UUID id, UUID userId, String name, LocalDate targetExamDate, Double dailyHours, PlanStrategy strategy, Boolean isActive, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        this.name = name != null ? name : "";
        this.targetExamDate = targetExamDate != null ? targetExamDate : LocalDate.now();
        this.dailyHours = dailyHours != null ? dailyHours : 2.0;
        this.strategy = strategy != null ? strategy : PlanStrategy.WEIGHTED;
        this.isActive = isActive != null ? isActive : true;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDate getTargetExamDate() { return targetExamDate; }
    public void setTargetExamDate(LocalDate targetExamDate) { this.targetExamDate = targetExamDate; }

    public Double getDailyHours() { return dailyHours; }
    public void setDailyHours(Double dailyHours) { this.dailyHours = dailyHours; }

    public PlanStrategy getStrategy() { return strategy; }
    public void setStrategy(PlanStrategy strategy) { this.strategy = strategy; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
