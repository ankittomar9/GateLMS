package com.gatelms.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "achievements",
    uniqueConstraints = [UniqueConstraint(name = "uq_achievement_code", columnNames = ["code"])]
)
class Achievement(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "code", nullable = false, length = 50, unique = true)
    val code: String,

    @Column(name = "earned_at", nullable = false)
    val earnedAt: Instant = Instant.now()
)
