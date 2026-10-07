package com.gatelms.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_goals")
class UserGoal(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "daily_target_questions", nullable = false)
    var dailyTargetQuestions: Int = 20,

    @Column(name = "streak_protection", nullable = false)
    var streakProtection: Boolean = true,

    @Column(name = "updated_at")
    var updatedAt: Instant = Instant.now()
)
