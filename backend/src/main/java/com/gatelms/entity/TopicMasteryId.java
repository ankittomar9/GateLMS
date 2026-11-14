package com.gatelms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class TopicMasteryId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "topic_id", length = 120)
    private String topicId;

    public TopicMasteryId() {}

    public TopicMasteryId(UUID userId, String topicId) {
        this.userId = userId;
        this.topicId = topicId;
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
