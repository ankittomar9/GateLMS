package com.gatelms.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "mock_exams")
class MockExam(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "title", length = 200, nullable = false)
    var title: String = "",

    @Column(name = "duration_minutes")
    var durationMinutes: Int? = 180,

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "total_marks")
    var totalMarks: Double? = 100.0,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blueprint_json", columnDefinition = "jsonb", nullable = false)
    var blueprintJson: String = "{}",

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", length = 20, nullable = false)
    var mode: ExamMode = ExamMode.MOCK,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
)
