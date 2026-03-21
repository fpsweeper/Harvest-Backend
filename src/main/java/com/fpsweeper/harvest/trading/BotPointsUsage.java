package com.fpsweeper.harvest.trading;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "bot_points_usage", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"bot_id", "usage_date"})
})
public class BotPointsUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bot_id", nullable = false)
    private UUID botId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "points_consumed", nullable = false, precision = 8, scale = 2)
    private BigDecimal pointsConsumed;

    @Column(name = "points_rate", nullable = false, precision = 6, scale = 2)
    private BigDecimal pointsRate;

    @Column(name = "bot_status", nullable = false, length = 20)
    private String botStatus;

    @Column(name = "hours_active", precision = 6, scale = 2)
    private BigDecimal hoursActive;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "consumption_details", columnDefinition = "jsonb")
    private Map<String, Object> consumptionDetails = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Constructors
    public BotPointsUsage() {
    }

    public BotPointsUsage(UUID botId, UUID userId, LocalDate usageDate, BigDecimal pointsConsumed, BigDecimal pointsRate, String botStatus) {
        this.botId = botId;
        this.userId = userId;
        this.usageDate = usageDate;
        this.pointsConsumed = pointsConsumed;
        this.pointsRate = pointsRate;
        this.botStatus = botStatus;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBotId() {
        return botId;
    }

    public void setBotId(UUID botId) {
        this.botId = botId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDate getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(LocalDate usageDate) {
        this.usageDate = usageDate;
    }

    public BigDecimal getPointsConsumed() {
        return pointsConsumed;
    }

    public void setPointsConsumed(BigDecimal pointsConsumed) {
        this.pointsConsumed = pointsConsumed;
    }

    public BigDecimal getPointsRate() {
        return pointsRate;
    }

    public void setPointsRate(BigDecimal pointsRate) {
        this.pointsRate = pointsRate;
    }

    public String getBotStatus() {
        return botStatus;
    }

    public void setBotStatus(String botStatus) {
        this.botStatus = botStatus;
    }

    public BigDecimal getHoursActive() {
        return hoursActive;
    }

    public void setHoursActive(BigDecimal hoursActive) {
        this.hoursActive = hoursActive;
    }

    public Map<String, Object> getConsumptionDetails() {
        return consumptionDetails;
    }

    public void setConsumptionDetails(Map<String, Object> consumptionDetails) {
        this.consumptionDetails = consumptionDetails;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}