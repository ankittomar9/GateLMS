package com.gatelms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    @Column(name = "title", length = 200, nullable = false)
    private String title = "";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate = LocalDate.now();

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate = LocalDate.now();

    @Column(name = "weekly_target_questions", nullable = false)
    private Integer weeklyTargetQuestions = 150;

    @Column(name = "weekly_target_minutes", nullable = false)
    private Integer weeklyTargetMinutes = 600;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", length = 30, nullable = false)
    private PlanStrategy strategy = PlanStrategy.WEIGHTED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_reasoning", columnDefinition = "jsonb")
    private String generatedReasoning;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public StudyPlan() {}

    public StudyPlan(UUID userId, String title, LocalDate startDate, LocalDate endDate, Integer weeklyTargetQuestions, Integer weeklyTargetMinutes, PlanStrategy strategy, String generatedReasoning) {
        this(UUID.randomUUID(), userId, title, startDate, endDate, weeklyTargetQuestions, weeklyTargetMinutes, strategy, generatedReasoning, Instant.now());
    }

    public StudyPlan(UUID id, UUID userId, String title, LocalDate startDate, LocalDate endDate, Integer weeklyTargetQuestions, Integer weeklyTargetMinutes, PlanStrategy strategy, String generatedReasoning, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        this.title = title != null ? title : "";
        this.startDate = startDate != null ? startDate : LocalDate.now();
        this.endDate = endDate != null ? endDate : LocalDate.now();
        this.weeklyTargetQuestions = weeklyTargetQuestions != null ? weeklyTargetQuestions : 150;
        this.weeklyTargetMinutes = weeklyTargetMinutes != null ? weeklyTargetMinutes : 600;
        this.strategy = strategy != null ? strategy : PlanStrategy.WEIGHTED;
        this.generatedReasoning = generatedReasoning;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getWeeklyTargetQuestions() { return weeklyTargetQuestions; }
    public void setWeeklyTargetQuestions(Integer weeklyTargetQuestions) { this.weeklyTargetQuestions = weeklyTargetQuestions; }

    public Integer getWeeklyTargetMinutes() { return weeklyTargetMinutes; }
    public void setWeeklyTargetMinutes(Integer weeklyTargetMinutes) { this.weeklyTargetMinutes = weeklyTargetMinutes; }

    public PlanStrategy getStrategy() { return strategy; }
    public void setStrategy(PlanStrategy strategy) { this.strategy = strategy; }

    public String getGeneratedReasoning() { return generatedReasoning; }
    public void setGeneratedReasoning(String generatedReasoning) { this.generatedReasoning = generatedReasoning; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
