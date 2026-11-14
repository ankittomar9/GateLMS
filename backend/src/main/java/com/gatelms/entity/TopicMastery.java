package com.gatelms.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TopicMasteryId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private UUID userId = UUID.randomUUID();

    @Column(name = "topic_id", length = 100, nullable = false)
    private String topicId = "";

    public TopicMasteryId() {}

    public TopicMasteryId(UUID userId, String topicId) {
        this.userId = userId != null ? userId : UUID.randomUUID();
        this.topicId = topicId != null ? topicId : "";
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTopicId() { return topicId; }
    public void setTopicId(String topicId) { this.topicId = topicId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicMasteryId that = (TopicMasteryId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(topicId, that.topicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, topicId);
    }
}

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
