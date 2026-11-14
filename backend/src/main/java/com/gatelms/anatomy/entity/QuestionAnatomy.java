package com.gatelms.anatomy.entity;

import com.gatelms.entity.Question;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "question_anatomy")
public class QuestionAnatomy {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private Question question;

    @Column(name = "tested_concept", columnDefinition = "TEXT", nullable = false)
    private String testedConcept = "";

    @Column(name = "invariant_rule", columnDefinition = "TEXT")
    private String invariantRule;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trap_analysis", columnDefinition = "jsonb", nullable = false)
    private String trapAnalysisJson = "[]";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prerequisites", columnDefinition = "jsonb", nullable = false)
    private String prerequisitesJson = "[]";

    @Column(name = "similar_pattern_hint", columnDefinition = "TEXT")
    private String similarPatternHint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "grounding_refs", columnDefinition = "jsonb", nullable = false)
    private String groundingRefsJson = "[]";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AnatomyStatus status = AnatomyStatus.DRAFT;

    @Column(name = "parse_error", columnDefinition = "TEXT")
    private String parseError;

    @Column(name = "generator_model", length = 100)
    private String generatorModel;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public QuestionAnatomy() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public String getTestedConcept() { return testedConcept; }
    public void setTestedConcept(String testedConcept) { this.testedConcept = testedConcept; }

    public String getInvariantRule() { return invariantRule; }
    public void setInvariantRule(String invariantRule) { this.invariantRule = invariantRule; }

    public String getTrapAnalysisJson() { return trapAnalysisJson; }
    public void setTrapAnalysisJson(String trapAnalysisJson) { this.trapAnalysisJson = trapAnalysisJson; }

    public String getPrerequisitesJson() { return prerequisitesJson; }
    public void setPrerequisitesJson(String prerequisitesJson) { this.prerequisitesJson = prerequisitesJson; }

    public String getSimilarPatternHint() { return similarPatternHint; }
    public void setSimilarPatternHint(String similarPatternHint) { this.similarPatternHint = similarPatternHint; }

    public String getGroundingRefsJson() { return groundingRefsJson; }
    public void setGroundingRefsJson(String groundingRefsJson) { this.groundingRefsJson = groundingRefsJson; }

    public AnatomyStatus getStatus() { return status; }
    public void setStatus(AnatomyStatus status) { this.status = status; }

    public String getParseError() { return parseError; }
    public void setParseError(String parseError) { this.parseError = parseError; }

    public String getGeneratorModel() { return generatorModel; }
    public void setGeneratorModel(String generatorModel) { this.generatorModel = generatorModel; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
