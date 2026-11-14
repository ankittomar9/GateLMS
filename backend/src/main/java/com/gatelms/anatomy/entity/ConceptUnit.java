package com.gatelms.anatomy.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "concept_units")
public class ConceptUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concept_tag_slug", referencedColumnName = "slug", nullable = false, unique = true)
    private ConceptTag conceptTag;

    @Column(name = "title", length = 300, nullable = false)
    private String title = "";

    @Column(name = "motivation", columnDefinition = "TEXT", nullable = false)
    private String motivation = "";

    @Column(name = "core_idea", columnDefinition = "TEXT", nullable = false)
    private String coreIdea = "";

    @Column(name = "invariant", columnDefinition = "TEXT")
    private String invariant;

    @Column(name = "worked_example", columnDefinition = "TEXT", nullable = false)
    private String workedExample = "";

    @Column(name = "worked_example_external_id", length = 120)
    private String workedExampleExternalId;

    @Column(name = "traps", columnDefinition = "TEXT")
    private String traps;

    @Column(name = "deep_link_ref", columnDefinition = "TEXT")
    private String deepLinkRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ConceptUnitStatus status = ConceptUnitStatus.DRAFT;

    @Column(name = "parse_error", columnDefinition = "TEXT")
    private String parseError;

    @Column(name = "generator_model", length = 100)
    private String generatorModel;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public ConceptUnit() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ConceptTag getConceptTag() { return conceptTag; }
    public void setConceptTag(ConceptTag conceptTag) { this.conceptTag = conceptTag; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }

    public String getCoreIdea() { return coreIdea; }
    public void setCoreIdea(String coreIdea) { this.coreIdea = coreIdea; }

    public String getInvariant() { return invariant; }
    public void setInvariant(String invariant) { this.invariant = invariant; }

    public String getWorkedExample() { return workedExample; }
    public void setWorkedExample(String workedExample) { this.workedExample = workedExample; }

    public String getWorkedExampleExternalId() { return workedExampleExternalId; }
    public void setWorkedExampleExternalId(String workedExampleExternalId) { this.workedExampleExternalId = workedExampleExternalId; }

    public String getTraps() { return traps; }
    public void setTraps(String traps) { this.traps = traps; }

    public String getDeepLinkRef() { return deepLinkRef; }
    public void setDeepLinkRef(String deepLinkRef) { this.deepLinkRef = deepLinkRef; }

    public ConceptUnitStatus getStatus() { return status; }
    public void setStatus(ConceptUnitStatus status) { this.status = status; }

    public String getParseError() { return parseError; }
    public void setParseError(String parseError) { this.parseError = parseError; }

    public String getGeneratorModel() { return generatorModel; }
    public void setGeneratorModel(String generatorModel) { this.generatorModel = generatorModel; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
