package com.gatelms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "achievements",
    uniqueConstraints = {@UniqueConstraint(name = "uq_user_achievement", columnNames = {"user_id", "code"})}
)
public class Achievement {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "earned_at", nullable = false)
    private Instant earnedAt = Instant.now();

    public Achievement() {}

    public Achievement(UUID userId, String code, Instant earnedAt) {
        this(UUID.randomUUID(), userId, code, earnedAt);
    }

    public Achievement(UUID userId, String code) {
        this(UUID.randomUUID(), userId, code, Instant.now());
    }

    public Achievement(UUID id, UUID userId, String code, Instant earnedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = userId != null ? userId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        this.code = code;
        this.earnedAt = earnedAt != null ? earnedAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Instant getEarnedAt() { return earnedAt; }
    public void setEarnedAt(Instant earnedAt) { this.earnedAt = earnedAt; }
}
