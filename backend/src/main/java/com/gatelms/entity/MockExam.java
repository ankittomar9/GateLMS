package com.gatelms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mock_exams")
public class MockExam {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "title", length = 200, nullable = false)
    private String title = "";

    @Column(name = "duration_minutes")
    private Integer durationMinutes = 180;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "total_marks")
    private Double totalMarks = 100.0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "blueprint_json", columnDefinition = "jsonb", nullable = false)
    private String blueprintJson = "{}";

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", length = 20, nullable = false)
    private ExamMode mode = ExamMode.MOCK;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public MockExam() {}

    public MockExam(UUID id, String title, Integer durationMinutes, Double totalMarks, String blueprintJson, ExamMode mode, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.title = title != null ? title : "";
        this.durationMinutes = durationMinutes != null ? durationMinutes : 180;
        this.totalMarks = totalMarks != null ? totalMarks : 100.0;
        this.blueprintJson = blueprintJson != null ? blueprintJson : "{}";
        this.mode = mode != null ? mode : ExamMode.MOCK;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Double getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Double totalMarks) { this.totalMarks = totalMarks; }

    public String getBlueprintJson() { return blueprintJson; }
    public void setBlueprintJson(String blueprintJson) { this.blueprintJson = blueprintJson; }

    public ExamMode getMode() { return mode; }
    public void setMode(ExamMode mode) { this.mode = mode; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
