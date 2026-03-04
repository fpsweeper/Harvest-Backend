package com.fpsweeper.harvest.points;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "point_transactions")
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String type;  // DEPOSIT, BOT_USAGE, REFUND, BONUS, ADMIN_ADJUSTMENT

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_before", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "deposit_id")
    private UUID depositId;

    @Column(name = "bot_execution_id")
    private UUID botExecutionId;

    @Column(nullable = false)
    private String description;

    @Column(name = "metadata", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // Constructors
    public PointTransaction() {}

    // Getters and Setters (generate all)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public UUID getDepositId() { return depositId; }
    public void setDepositId(UUID depositId) { this.depositId = depositId; }

    public UUID getBotExecutionId() { return botExecutionId; }
    public void setBotExecutionId(UUID botExecutionId) {
        this.botExecutionId = botExecutionId;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
}