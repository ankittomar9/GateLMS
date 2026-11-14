package com.gatelms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "plan_items")
public class PlanItem {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private StudyPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate = LocalDate.now();

    @Column(name = "target_questions", nullable = false)
    private Integer targetQuestions = 10;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private PlanItemStatus status = PlanItemStatus.OPEN;

    @Column(name = "completed_questions", nullable = false)
    private Integer completedQuestions = 0;

    public PlanItem() {}

    public PlanItem(UUID id, StudyPlan plan, Topic topic, LocalDate targetDate, Integer targetQuestions, PlanItemStatus status, Integer completedQuestions) {
        this.id = id != null ? id : UUID.randomUUID();
        this.plan = plan;
        this.topic = topic;
        this.targetDate = targetDate != null ? targetDate : LocalDate.now();
        this.targetQuestions = targetQuestions != null ? targetQuestions : 10;
        this.status = status != null ? status : PlanItemStatus.OPEN;
        this.completedQuestions = completedQuestions != null ? completedQuestions : 0;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public StudyPlan getPlan() { return plan; }
    public void setPlan(StudyPlan plan) { this.plan = plan; }

    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public Integer getTargetQuestions() { return targetQuestions; }
    public void setTargetQuestions(Integer targetQuestions) { this.targetQuestions = targetQuestions; }

    public PlanItemStatus getStatus() { return status; }
    public void setStatus(PlanItemStatus status) { this.status = status; }

    public Integer getCompletedQuestions() { return completedQuestions; }
    public void setCompletedQuestions(Integer completedQuestions) { this.completedQuestions = completedQuestions; }
}
