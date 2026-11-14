package com.gatelms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "external_id", length = 100, unique = true)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Column(name = "year")
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private QuestionType type = QuestionType.MCQ;

    @Column(name = "marks", nullable = false)
    private Integer marks = 1;

    @Column(name = "statement", columnDefinition = "TEXT", nullable = false)
    private String statement = "";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "jsonb")
    private String optionsJson;

    @Column(name = "correct_options", length = 50)
    private String correctOptions;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "numeric_min")
    private Double numericMin;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "numeric_max")
    private Double numericMax;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "gate_overflow_url", length = 500)
    private String gateOverflowUrl;

    @Column(name = "source", length = 30, nullable = false)
    private String source = "SAMPLE";

    @Column(name = "curation_status", length = 20, nullable = false)
    private String curationStatus = "UNREVIEWED";

    @Column(name = "curation_note", columnDefinition = "TEXT")
    private String curationNote;

    @Column(name = "difficulty", length = 10)
    private String difficulty;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public Question() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public QuestionType getType() { return type; }
    public void setType(QuestionType type) { this.type = type; }

    public Integer getMarks() { return marks; }
    public void setMarks(Integer marks) { this.marks = marks; }

    public String getStatement() { return statement; }
    public void setStatement(String statement) { this.statement = statement; }

    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }

    public String getCorrectOptions() { return correctOptions; }
    public void setCorrectOptions(String correctOptions) { this.correctOptions = correctOptions; }

    public Double getNumericMin() { return numericMin; }
    public void setNumericMin(Double numericMin) { this.numericMin = numericMin; }

    public Double getNumericMax() { return numericMax; }
    public void setNumericMax(Double numericMax) { this.numericMax = numericMax; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getGateOverflowUrl() { return gateOverflowUrl; }
    public void setGateOverflowUrl(String gateOverflowUrl) { this.gateOverflowUrl = gateOverflowUrl; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCurationStatus() { return curationStatus; }
    public void setCurationStatus(String curationStatus) { this.curationStatus = curationStatus; }

    public String getCurationNote() { return curationNote; }
    public void setCurationNote(String curationNote) { this.curationNote = curationNote; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
