package com.gatelms.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "achievements",
    uniqueConstraints = [UniqueConstraint(name = "uq_user_achievement", columnNames = ["user_id", "code"])]
)
class Achievement(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),

    @Column(name = "code", nullable = false, length = 50)
    val code: String,

    @Column(name = "earned_at", nullable = false)
    val earnedAt: Instant = Instant.now()
)
