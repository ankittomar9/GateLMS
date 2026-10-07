package com.gatelms.entity

import jakarta.persistence.*
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notes")
class Note(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),

    @Column(name = "title", nullable = false, length = 255)
    var title: String,

    @Column(name = "file_path", length = 500)
    var filePath: String? = null,

    @Column(name = "original_filename", length = 255)
    var originalFilename: String? = null,

    @Column(name = "content_type", length = 100)
    var contentType: String? = null,

    @Column(name = "file_size_bytes")
    var fileSizeBytes: Long? = null,

    @Column(name = "content_md", columnDefinition = "TEXT")
    var contentMd: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    var subject: Subject? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    var topic: Topic? = null,

    @Column(name = "is_pinned", nullable = false)
    var isPinned: Boolean = false,

    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    val kind: String
        get() = if (filePath != null) "FILE" else "TEXT"
}
