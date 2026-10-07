package com.gatelms.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "username", length = 50, nullable = false, unique = true)
    var username: String = "",

    @Column(name = "password_hash", length = 100, nullable = false)
    var passwordHash: String = "",

    @Column(name = "display_name", length = 100, nullable = false)
    var displayName: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    var role: UserRole = UserRole.STUDENT,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
