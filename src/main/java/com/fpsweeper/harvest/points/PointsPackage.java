package com.fpsweeper.harvest.points;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a purchasable points package shown on the landing page and wallet.
 * Stored in DB so prices/points can be updated without redeploying.
 */
@Entity
@Table(name = "points_packages")
public class PointsPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal points;

    @Column(name = "price_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceUsd;

    @Column(name = "is_popular", nullable = false)
    private boolean popular = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PointsPackage() {}

    // ── Getters & Setters ──────────────────────────────────────────────────

    public UUID getId()                       { return id; }
    public String getName()                   { return name; }
    public void setName(String n)             { this.name = n; }
    public String getDescription()            { return description; }
    public void setDescription(String d)      { this.description = d; }
    public BigDecimal getPoints()             { return points; }
    public void setPoints(BigDecimal p)       { this.points = p; }
    public BigDecimal getPriceUsd()           { return priceUsd; }
    public void setPriceUsd(BigDecimal p)     { this.priceUsd = p; }
    public boolean isPopular()                { return popular; }
    public void setPopular(boolean p)         { this.popular = p; }
    public boolean isActive()                 { return active; }
    public void setActive(boolean a)          { this.active = a; }
    public int getSortOrder()                 { return sortOrder; }
    public void setSortOrder(int s)           { this.sortOrder = s; }
    public Instant getCreatedAt()             { return createdAt; }
}