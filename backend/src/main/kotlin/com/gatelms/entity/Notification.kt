package com.gatelms.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notifications")
class Notification(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    val userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    val type: NotificationType,

    @Column(name = "title", nullable = false, length = 200)
    val title: String,

    @Column(name = "body", columnDefinition = "TEXT")
    val body: String? = null,

    @Column(name = "link", length = 300)
    val link: String? = null,

    @Column(name = "read_at")
    var readAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
)
