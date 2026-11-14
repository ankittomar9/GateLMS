package com.gatelms.anatomy.entity;

import com.gatelms.entity.Topic;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "concept_tags")
public class ConceptTag {
    @Id
    @Column(name = "slug", length = 120, nullable = false)
    private String slug = "";

    @Column(name = "label", length = 200, nullable = false)
    private String label = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ConceptTag() {}

    public ConceptTag(String slug, String label, Topic topic) {
        this(slug, label, topic, Instant.now());
    }

    public ConceptTag(String slug, String label, Topic topic, Instant createdAt) {
        this.slug = slug != null ? slug : "";
        this.label = label != null ? label : "";
        this.topic = topic;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public Topic getTopic() { return topic; }
    public void setTopic(Topic topic) { this.topic = topic; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
