package com.gatelms.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "question_progress")
public class QuestionProgress {
    @EmbeddedId
    private QuestionProgressId id = new QuestionProgressId();

    @Enumerated(EnumType.STRING)
    @Column(name = "pass1_status", length = 20, nullable = false)
    private Pass1Status pass1Status = Pass1Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "pass2_status", length = 20, nullable = false)
    private Pass2Status pass2Status = Pass2Status.PENDING;

    @Column(name = "is_bookmarked", nullable = false)
    private Boolean isBookmarked = false;

    @Column(name = "bookmarked_at")
    private Instant bookmarkedAt;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public QuestionProgress() {}

    public QuestionProgress(QuestionProgressId id) {
        this.id = id != null ? id : new QuestionProgressId();
    }

    public QuestionProgress(QuestionProgressId id, Pass1Status pass1Status, Pass2Status pass2Status, Boolean isBookmarked, Instant bookmarkedAt, Instant lastAttemptedAt) {
        this.id = id != null ? id : new QuestionProgressId();
        this.pass1Status = pass1Status != null ? pass1Status : Pass1Status.PENDING;
        this.pass2Status = pass2Status != null ? pass2Status : Pass2Status.PENDING;
        this.isBookmarked = isBookmarked != null ? isBookmarked : false;
        this.bookmarkedAt = bookmarkedAt;
        this.lastAttemptedAt = lastAttemptedAt;
    }

    public QuestionProgressId getId() { return id; }
    public void setId(QuestionProgressId id) { this.id = id; }

    public Pass1Status getPass1Status() { return pass1Status; }
    public void setPass1Status(Pass1Status pass1Status) { this.pass1Status = pass1Status; }

    public Pass2Status getPass2Status() { return pass2Status; }
    public void setPass2Status(Pass2Status pass2Status) { this.pass2Status = pass2Status; }

    public Boolean getIsBookmarked() { return isBookmarked; }
    public boolean isBookmarked() { return Boolean.TRUE.equals(isBookmarked); }
    public void setIsBookmarked(Boolean isBookmarked) { this.isBookmarked = isBookmarked; }

    public Instant getBookmarkedAt() { return bookmarkedAt; }
    public void setBookmarkedAt(Instant bookmarkedAt) { this.bookmarkedAt = bookmarkedAt; }

    public Instant getLastAttemptedAt() { return lastAttemptedAt; }
    public void setLastAttemptedAt(Instant lastAttemptedAt) { this.lastAttemptedAt = lastAttemptedAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
