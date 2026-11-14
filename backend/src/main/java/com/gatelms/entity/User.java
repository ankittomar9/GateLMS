package com.gatelms.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username = "";

    @Column(name = "password_hash", length = 100, nullable = false)
    private String passwordHash = "";

    @Column(name = "display_name", length = 100, nullable = false)
    private String displayName = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private UserRole role = UserRole.STUDENT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public User() {}

    public User(UUID id, String username, String passwordHash, String displayName, UserRole role, Instant createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.username = username != null ? username : "";
        this.passwordHash = passwordHash != null ? passwordHash : "";
        this.displayName = displayName != null ? displayName : "";
        this.role = role != null ? role : UserRole.STUDENT;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
