package com.gatelms.anatomy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RetrievalCardStateId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "card_id")
    private UUID cardId;

    public RetrievalCardStateId() {}

    public RetrievalCardStateId(UUID userId, UUID cardId) {
        this.userId = userId;
        this.cardId = cardId;
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
