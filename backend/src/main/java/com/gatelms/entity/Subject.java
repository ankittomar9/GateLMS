package com.gatelms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subjects")
public class Subject {
    @Id
    @Column(name = "id", length = 50, nullable = false)
    private String id = "";

    @Column(name = "name", length = 150, nullable = false)
    private String name = "";

    @Column(name = "slug", length = 100, nullable = false, unique = true)
    private String slug = "";

    @Column(name = "cluster", length = 50, nullable = false)
    private String cluster = "";

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(name = "avg_weightage", nullable = false)
    private Double avgWeightage = 0.0;

    @Column(name = "priority_tier", nullable = false)
    private Integer priorityTier = 1;

    @Column(name = "standard_book_ref", length = 255)
    private String standardBookRef;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Topic> topics = new ArrayList<>();

    public Subject() {}

    public Subject(String id, String name, String slug, String cluster, Double avgWeightage, Integer priorityTier, String standardBookRef, Instant createdAt) {
        this.id = id != null ? id : "";
        this.name = name != null ? name : "";
        this.slug = slug != null ? slug : "";
        this.cluster = cluster != null ? cluster : "";
        this.avgWeightage = avgWeightage != null ? avgWeightage : 0.0;
        this.priorityTier = priorityTier != null ? priorityTier : 1;
        this.standardBookRef = standardBookRef;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getCluster() { return cluster; }
    public void setCluster(String cluster) { this.cluster = cluster; }

    public Double getAvgWeightage() { return avgWeightage; }
    public void setAvgWeightage(Double avgWeightage) { this.avgWeightage = avgWeightage; }

    public Integer getPriorityTier() { return priorityTier; }
    public void setPriorityTier(Integer priorityTier) { this.priorityTier = priorityTier; }

    public String getStandardBookRef() { return standardBookRef; }
    public void setStandardBookRef(String standardBookRef) { this.standardBookRef = standardBookRef; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public List<Topic> getTopics() { return topics; }
    public void setTopics(List<Topic> topics) { this.topics = topics; }
}
