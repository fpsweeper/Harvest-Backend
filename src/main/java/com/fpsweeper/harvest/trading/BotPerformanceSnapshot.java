package com.fpsweeper.harvest.trading;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "bot_performance_snapshots")
public class BotPerformanceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bot_id", nullable = false)
    private UUID botId;

    @Column(name = "snapshot_time", nullable = false)
    private Instant snapshotTime = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", nullable = false, length = 20)
    private SnapshotType snapshotType = SnapshotType.HOURLY;

    // Balance Metrics
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;

    @Column(name = "initial_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal initialBalance;

    // P&L Metrics
    @Column(name = "total_pnl", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPnl = BigDecimal.ZERO;

    @Column(name = "realized_pnl", nullable = false, precision = 18, scale = 2)
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "unrealized_pnl", nullable = false, precision = 18, scale = 2)
    private BigDecimal unrealizedPnl = BigDecimal.ZERO;

    @Column(name = "total_pnl_percentage", precision = 8, scale = 4)
    private BigDecimal totalPnlPercentage;

    // Trading Metrics
    @Column(name = "total_trades", nullable = false)
    private Integer totalTrades = 0;

    @Column(name = "winning_trades", nullable = false)
    private Integer winningTrades = 0;

    @Column(name = "losing_trades", nullable = false)
    private Integer losingTrades = 0;

    @Column(name = "win_rate", precision = 5, scale = 2)
    private BigDecimal winRate;

    // Position Metrics
    @Column(name = "open_positions_count", nullable = false)
    private Integer openPositionsCount = 0;

    @Column(name = "open_positions_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal openPositionsValue = BigDecimal.ZERO;

    // Risk Metrics
    @Column(name = "max_drawdown", precision = 8, scale = 4)
    private BigDecimal maxDrawdown;

    @Column(name = "max_drawdown_percentage", precision = 8, scale = 4)
    private BigDecimal maxDrawdownPercentage;

    // Trade Performance
    @Column(name = "average_win", precision = 18, scale = 2)
    private BigDecimal averageWin;

    @Column(name = "average_loss", precision = 18, scale = 2)
    private BigDecimal averageLoss;

    @Column(name = "largest_win", precision = 18, scale = 2)
    private BigDecimal largestWin;

    @Column(name = "largest_loss", precision = 18, scale = 2)
    private BigDecimal largestLoss;

    @Column(name = "profit_factor", precision = 8, scale = 4)
    private BigDecimal profitFactor;

    // Additional Metrics
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_metrics", columnDefinition = "jsonb")
    private Map<String, Object> additionalMetrics = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Constructors
    public BotPerformanceSnapshot() {
    }

    public BotPerformanceSnapshot(UUID botId, BigDecimal balance, BigDecimal initialBalance) {
        this.botId = botId;
        this.balance = balance;
        this.initialBalance = initialBalance;
        this.totalPnl = balance.subtract(initialBalance);

        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            this.totalPnlPercentage = totalPnl
                    .divide(initialBalance, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
    }

    // Helper methods
    public void calculateWinRate() {
        if (totalTrades > 0) {
            this.winRate = new BigDecimal(winningTrades)
                    .divide(new BigDecimal(totalTrades), 2, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
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

    public Instant getSnapshotTime() {
        return snapshotTime;
    }

    public void setSnapshotTime(Instant snapshotTime) {
        this.snapshotTime = snapshotTime;
    }

    public SnapshotType getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(SnapshotType snapshotType) {
        this.snapshotType = snapshotType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public BigDecimal getTotalPnl() {
        return totalPnl;
    }

    public void setTotalPnl(BigDecimal totalPnl) {
        this.totalPnl = totalPnl;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }

    public BigDecimal getTotalPnlPercentage() {
        return totalPnlPercentage;
    }

    public void setTotalPnlPercentage(BigDecimal totalPnlPercentage) {
        this.totalPnlPercentage = totalPnlPercentage;
    }

    public Integer getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    public Integer getWinningTrades() {
        return winningTrades;
    }

    public void setWinningTrades(Integer winningTrades) {
        this.winningTrades = winningTrades;
    }

    public Integer getLosingTrades() {
        return losingTrades;
    }

    public void setLosingTrades(Integer losingTrades) {
        this.losingTrades = losingTrades;
    }

    public BigDecimal getWinRate() {
        return winRate;
    }

    public void setWinRate(BigDecimal winRate) {
        this.winRate = winRate;
    }

    public Integer getOpenPositionsCount() {
        return openPositionsCount;
    }

    public void setOpenPositionsCount(Integer openPositionsCount) {
        this.openPositionsCount = openPositionsCount;
    }

    public BigDecimal getOpenPositionsValue() {
        return openPositionsValue;
    }

    public void setOpenPositionsValue(BigDecimal openPositionsValue) {
        this.openPositionsValue = openPositionsValue;
    }

    public BigDecimal getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(BigDecimal maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    public BigDecimal getMaxDrawdownPercentage() {
        return maxDrawdownPercentage;
    }

    public void setMaxDrawdownPercentage(BigDecimal maxDrawdownPercentage) {
        this.maxDrawdownPercentage = maxDrawdownPercentage;
    }

    public BigDecimal getAverageWin() {
        return averageWin;
    }

    public void setAverageWin(BigDecimal averageWin) {
        this.averageWin = averageWin;
    }

    public BigDecimal getAverageLoss() {
        return averageLoss;
    }

    public void setAverageLoss(BigDecimal averageLoss) {
        this.averageLoss = averageLoss;
    }

    public BigDecimal getLargestWin() {
        return largestWin;
    }

    public void setLargestWin(BigDecimal largestWin) {
        this.largestWin = largestWin;
    }

    public BigDecimal getLargestLoss() {
        return largestLoss;
    }

    public void setLargestLoss(BigDecimal largestLoss) {
        this.largestLoss = largestLoss;
    }

    public BigDecimal getProfitFactor() {
        return profitFactor;
    }

    public void setProfitFactor(BigDecimal profitFactor) {
        this.profitFactor = profitFactor;
    }

    public Map<String, Object> getAdditionalMetrics() {
        return additionalMetrics;
    }

    public void setAdditionalMetrics(Map<String, Object> additionalMetrics) {
        this.additionalMetrics = additionalMetrics;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}