package com.gatelms.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(
    name = "topics",
    uniqueConstraints = {@UniqueConstraint(name = "uq_subject_topic", columnNames = {"subject_id", "slug"})}
)
public class Topic {
    @Id
    @Column(name = "id", length = 100, nullable = false)
    private String id = "";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "name", length = 200, nullable = false)
    private String name = "";

    @Column(name = "slug", length = 150, nullable = false)
    private String slug = "";

    @Column(name = "book_chapter_ref", length = 255)
    private String bookChapterRef;

    @Column(name = "sequence", nullable = false)
    private Integer sequence = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public Topic() {}

    public Topic(String id, Subject subject, String name, String slug, String bookChapterRef, Integer sequence, Instant createdAt, Instant updatedAt, Integer version) {
        this.id = id != null ? id : "";
        this.subject = subject;
        this.name = name != null ? name : "";
        this.slug = slug != null ? slug : "";
        this.bookChapterRef = bookChapterRef;
        this.sequence = sequence != null ? sequence : 0;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt;
        this.version = version != null ? version : 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getBookChapterRef() { return bookChapterRef; }
    public void setBookChapterRef(String bookChapterRef) { this.bookChapterRef = bookChapterRef; }

    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
