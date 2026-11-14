package com.gatelms.anatomy.entity;

import com.gatelms.entity.Topic;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "topic_primers")
public class TopicPrimer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", referencedColumnName = "id", nullable = false, unique = true)
    private Topic topic;

    @Column(name = "motivation", columnDefinition = "TEXT", nullable = false)
    private String motivation = "";

    @Column(name = "core_idea", columnDefinition = "TEXT", nullable = false)
    private String coreIdea = "";

    @Column(name = "invariant", columnDefinition = "TEXT")
    private String invariant;

    @Column(name = "worked_example", columnDefinition = "TEXT", nullable = false)
    private String workedExample = "";

    @Column(name = "traps", columnDefinition = "TEXT")
    private String traps;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TopicPrimerStatus status = TopicPrimerStatus.DRAFT;

    @Column(name = "parse_error", columnDefinition = "TEXT")
    private String parseError;

    @Column(name = "generator_model", length = 100)
    private String generatorModel;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt = Instant.now();

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public TopicPrimer() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }

    public String getMotivation() { return motivation; }
    public void setMotivation(String motivation) { this.motivation = motivation; }

    public String getCoreIdea() { return coreIdea; }
    public void setCoreIdea(String coreIdea) { this.coreIdea = coreIdea; }

    public String getInvariant() { return invariant; }
    public void setInvariant(String invariant) { this.invariant = invariant; }

    public String getWorkedExample() { return workedExample; }
    public void setWorkedExample(String workedExample) { this.workedExample = workedExample; }

    public String getTraps() { return traps; }
    public void setTraps(String traps) { this.traps = traps; }

    public TopicPrimerStatus getStatus() { return status; }
    public void setStatus(TopicPrimerStatus status) { this.status = status; }

    public String getParseError() { return parseError; }
    public void setParseError(String parseError) { this.parseError = parseError; }

    public String getGeneratorModel() { return generatorModel; }
    public void setGeneratorModel(String generatorModel) { this.generatorModel = generatorModel; }

    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
