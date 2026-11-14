package com.gatelms.anatomy.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RetrievalCardStateId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private UUID userId = UUID.randomUUID();

    @Column(name = "card_id", nullable = false)
    private UUID cardId = UUID.randomUUID();

    public RetrievalCardStateId() {}

    public RetrievalCardStateId(UUID userId, UUID cardId) {
        this.userId = userId != null ? userId : UUID.randomUUID();
        this.cardId = cardId != null ? cardId : UUID.randomUUID();
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RetrievalCardStateId that = (RetrievalCardStateId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(cardId, that.cardId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, cardId);
    }
}

@Entity
@Table(name = "retrieval_card_state")
public class RetrievalCardState {
    @EmbeddedId
    private RetrievalCardStateId id = new RetrievalCardStateId();

    @Column(name = "due_at", nullable = false)
    private Instant dueAt = Instant.now();

    @Column(name = "interval_days", nullable = false)
    private Float intervalDays = 0f;

    @Column(name = "ease", nullable = false)
    private Float ease = 2.5f;

    @Column(name = "consecutive_correct", nullable = false)
    private Integer consecutiveCorrect = 0;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    public RetrievalCardState() {}

    public RetrievalCardState(RetrievalCardStateId id, Instant dueAt, Float intervalDays, Float ease, Integer consecutiveCorrect, Instant lastReviewedAt) {
        this.id = id != null ? id : new RetrievalCardStateId();
        this.dueAt = dueAt != null ? dueAt : Instant.now();
        this.intervalDays = intervalDays != null ? intervalDays : 0f;
        this.ease = ease != null ? ease : 2.5f;
        this.consecutiveCorrect = consecutiveCorrect != null ? consecutiveCorrect : 0;
        this.lastReviewedAt = lastReviewedAt;
    }

    public RetrievalCardStateId getId() { return id; }
    public void setId(RetrievalCardStateId id) { this.id = id; }

    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }

    public Float getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Float intervalDays) { this.intervalDays = intervalDays; }

    public Float getEase() { return ease; }
    public void setEase(Float ease) { this.ease = ease; }

    public Integer getConsecutiveCorrect() { return consecutiveCorrect; }
    public void setConsecutiveCorrect(Integer consecutiveCorrect) { this.consecutiveCorrect = consecutiveCorrect; }

    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(Instant lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
}
