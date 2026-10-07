package com.gatelms.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "plan_items",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["plan_id", "plan_date", "topic_id"])
    ]
)
class PlanItem(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    var plan: StudyPlan,

    @Column(name = "plan_date", nullable = false)
    var planDate: LocalDate = LocalDate.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    var subject: Subject? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    var topic: Topic? = null,

    @Column(name = "target_questions", nullable = false)
    var targetQuestions: Int = 10,

    @Column(name = "target_minutes", nullable = false)
    var targetMinutes: Int = 30,

    @Column(name = "done_questions", nullable = false)
    var doneQuestions: Int = 0,

    @Column(name = "done_minutes", nullable = false)
    var doneMinutes: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10, nullable = false)
    var status: PlanItemStatus = PlanItemStatus.OPEN
)
