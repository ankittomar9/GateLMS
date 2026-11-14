package com.gatelms.entity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "topic_mastery")
public class TopicMastery {
    @EmbeddedId
    private TopicMasteryId id = new TopicMasteryId();

    @Column(name = "mastery_level", nullable = false)
    private Integer masteryLevel = 0;

    @Column(name = "last_revised_at")
    private Instant lastRevisedAt;

    @Column(name = "next_revision_due")
    private Instant nextRevisionDue;

    public TopicMastery() {}

    public TopicMastery(TopicMasteryId id) {
        this(id, 0, null, null);
    }

    public TopicMastery(TopicMasteryId id, Integer masteryLevel) {
        this(id, masteryLevel, null, null);
    }

    public TopicMastery(TopicMasteryId id, Integer masteryLevel, Instant lastRevisedAt, Instant nextRevisionDue) {
        this.id = id != null ? id : new TopicMasteryId();
        this.masteryLevel = masteryLevel != null ? masteryLevel : 0;
        this.lastRevisedAt = lastRevisedAt;
        this.nextRevisionDue = nextRevisionDue;
    }

    public TopicMasteryId getId() { return id; }
    public void setId(TopicMasteryId id) { this.id = id; }

    public Integer getMasteryLevel() { return masteryLevel; }
    public void setMasteryLevel(Integer masteryLevel) { this.masteryLevel = masteryLevel; }

    public Instant getLastRevisedAt() { return lastRevisedAt; }
    public void setLastRevisedAt(Instant lastRevisedAt) { this.lastRevisedAt = lastRevisedAt; }

    public Instant getNextRevisionDue() { return nextRevisionDue; }
    public void setNextRevisionDue(Instant nextRevisionDue) { this.nextRevisionDue = nextRevisionDue; }
}
