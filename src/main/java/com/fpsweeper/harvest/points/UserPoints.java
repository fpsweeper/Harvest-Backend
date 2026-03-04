package com.fpsweeper.harvest.points;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_points")
public class UserPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal points = BigDecimal.ZERO;

    @Column(name = "last_updated")
    private Instant lastUpdated = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        lastUpdated = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = Instant.now();
    }

    // Constructors
    public UserPoints() {}

    public UserPoints(UUID userId) {
        this.userId = userId;
        this.points = BigDecimal.ZERO;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}