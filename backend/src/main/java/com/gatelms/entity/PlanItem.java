package com.gatelms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
    name = "plan_items",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"plan_id", "plan_date", "topic_id"})
    }
)
public class PlanItem {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private StudyPlan plan;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "target_questions", nullable = false)
    private Integer targetQuestions = 10;

    @Column(name = "target_minutes", nullable = false)
    private Integer targetMinutes = 30;

    @Column(name = "done_questions", nullable = false)
    private Integer doneQuestions = 0;

    @Column(name = "done_minutes", nullable = false)
    private Integer doneMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    private PlanItemStatus status = PlanItemStatus.OPEN;

    public PlanItem() {}

    public PlanItem(StudyPlan plan, LocalDate planDate, Subject subject, Topic topic, int targetQuestions, int targetMinutes, int doneQuestions, int doneMinutes, PlanItemStatus status) {
        this(UUID.randomUUID(), plan, planDate, subject, topic, targetQuestions, targetMinutes, doneQuestions, doneMinutes, status);
    }

    public PlanItem(UUID id, StudyPlan plan, LocalDate planDate, Subject subject, Topic topic, Integer targetQuestions, Integer targetMinutes, Integer doneQuestions, Integer doneMinutes, PlanItemStatus status) {
        this.id = id != null ? id : UUID.randomUUID();
        this.plan = plan;
        this.planDate = planDate != null ? planDate : LocalDate.now();
        this.subject = subject;
        this.topic = topic;
        this.targetQuestions = targetQuestions != null ? targetQuestions : 10;
        this.targetMinutes = targetMinutes != null ? targetMinutes : 30;
        this.doneQuestions = doneQuestions != null ? doneQuestions : 0;
        this.doneMinutes = doneMinutes != null ? doneMinutes : 0;
        this.status = status != null ? status : PlanItemStatus.OPEN;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public StudyPlan getPlan() { return plan; }
    public void setPlan(StudyPlan plan) { this.plan = plan; }

    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate planDate) { this.planDate = planDate; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }

    public Integer getTargetQuestions() { return targetQuestions; }
    public void setTargetQuestions(Integer targetQuestions) { this.targetQuestions = targetQuestions; }

    public Integer getTargetMinutes() { return targetMinutes; }
    public void setTargetMinutes(Integer targetMinutes) { this.targetMinutes = targetMinutes; }

    public Integer getDoneQuestions() { return doneQuestions; }
    public void setDoneQuestions(Integer doneQuestions) { this.doneQuestions = doneQuestions; }

    public Integer getDoneMinutes() { return doneMinutes; }
    public void setDoneMinutes(Integer doneMinutes) { this.doneMinutes = doneMinutes; }

    public PlanItemStatus getStatus() { return status; }
    public void setStatus(PlanItemStatus status) { this.status = status; }
}
