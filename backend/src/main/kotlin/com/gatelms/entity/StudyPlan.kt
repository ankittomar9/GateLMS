package com.gatelms.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "study_plans")
class StudyPlan(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),

    @Column(name = "title", length = 200, nullable = false)
    var title: String = "",

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = LocalDate.now(),

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate = LocalDate.now(),

    @Column(name = "weekly_target_questions", nullable = false)
    var weeklyTargetQuestions: Int = 150,

    @Column(name = "weekly_target_minutes", nullable = false)
    var weeklyTargetMinutes: Int = 600,

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", length = 30, nullable = false)
    var strategy: PlanStrategy = PlanStrategy.WEIGHTED,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "generated_reasoning", columnDefinition = "jsonb")
    var generatedReasoning: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
