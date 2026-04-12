package com.fpsweeper.harvest.trading;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "trading_bots")
public class TradingBot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false, length = 50)
    private StrategyType strategyType;

    @Column(name = "trading_pair", nullable = false, length = 20)
    private String tradingPair;

    @Column(nullable = false, length = 10)
    private String timeframe;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BotStatus status = BotStatus.CREATED;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> configuration = new HashMap<>();


    @Column(name = "initial_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal initialBalance = new BigDecimal("1000.00");

    @Column(name = "current_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal currentBalance = new BigDecimal("1000.00");

    @Column(name = "stop_loss_percentage", precision = 5, scale = 2)
    private BigDecimal stopLossPercentage;

    @Column(name = "take_profit_percentage", precision = 5, scale = 2)
    private BigDecimal takeProfitPercentage;

    @Column(name = "max_position_size_percentage", precision = 5, scale = 2)
    private BigDecimal maxPositionSizePercentage = new BigDecimal("20.00");

    @Column(name = "daily_loss_limit_percentage", precision = 5, scale = 2)
    private BigDecimal dailyLossLimitPercentage = new BigDecimal("10.00");

    @Column(name = "points_per_day", nullable = false, precision = 6, scale = 2)
    private BigDecimal pointsPerDay = new BigDecimal("1.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "trading_mode", nullable = false, length = 20)
    private TradingMode tradingMode = TradingMode.SIMULATION;

    /**
     * true  = balance is virtual credit (free $1000 granted by platform)
     * false = balance came from real user deposit (LIVE mode only, future)
     */
    @Column(name = "virtual_credit", nullable = false)
    private boolean virtualCredit = true;

    @Column(name = "total_points_consumed", precision = 10, scale = 2)
    private BigDecimal totalPointsConsumed = BigDecimal.ZERO;

    @Column(name = "last_execution_time")
    private Instant lastExecutionTime;

    @Column(name = "next_execution_time")
    private Instant nextExecutionTime;

    @Column(name = "execution_count")
    private Integer executionCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "paused_at")
    private Instant pausedAt;

    @Column(name = "stopped_at")
    private Instant stoppedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // Relationships
    @OneToMany(mappedBy = "botId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BotIndicatorCondition> conditions = new ArrayList<>();

    @OneToMany(mappedBy = "botId", cascade = CascadeType.ALL)
    private List<BotTrade> trades = new ArrayList<>();

    @OneToMany(mappedBy = "botId", cascade = CascadeType.ALL)
    private List<BotPosition> positions = new ArrayList<>();

    // Constructors
    public TradingBot() {
    }

    // Helper methods
    public boolean isActive() {
        return status == BotStatus.SIMULATING;
    }

    public boolean canStart() {
        return status == BotStatus.CREATED || status == BotStatus.PAUSED;
    }

    public boolean canPause() {
        return status == BotStatus.SIMULATING;
    }

    public boolean canStop() {
        return status == BotStatus.SIMULATING || status == BotStatus.PAUSED;
    }

    public boolean canDelete() {
        if (status != BotStatus.STOPPED) {
            return false;
        }
        // Can only delete 24 hours after stopping
        return stoppedAt != null &&
                Instant.now().isAfter(stoppedAt.plusSeconds(24 * 3600));
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public StrategyType getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(StrategyType strategyType) {
        this.strategyType = strategyType;
    }

    public String getTradingPair() {
        return tradingPair;
    }

    public void setTradingPair(String tradingPair) {
        this.tradingPair = tradingPair;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public BotStatus getStatus() {
        return status;
    }

    public void setStatus(BotStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getStopLossPercentage() {
        return stopLossPercentage;
    }

    public void setStopLossPercentage(BigDecimal stopLossPercentage) {
        this.stopLossPercentage = stopLossPercentage;
    }

    public BigDecimal getTakeProfitPercentage() {
        return takeProfitPercentage;
    }

    public void setTakeProfitPercentage(BigDecimal takeProfitPercentage) {
        this.takeProfitPercentage = takeProfitPercentage;
    }

    public BigDecimal getMaxPositionSizePercentage() {
        return maxPositionSizePercentage;
    }

    public void setMaxPositionSizePercentage(BigDecimal maxPositionSizePercentage) {
        this.maxPositionSizePercentage = maxPositionSizePercentage;
    }

    public BigDecimal getDailyLossLimitPercentage() {
        return dailyLossLimitPercentage;
    }

    public void setDailyLossLimitPercentage(BigDecimal dailyLossLimitPercentage) {
        this.dailyLossLimitPercentage = dailyLossLimitPercentage;
    }

    public BigDecimal getPointsPerDay() {
        return pointsPerDay;
    }

    public void setPointsPerDay(BigDecimal pointsPerDay) {
        this.pointsPerDay = pointsPerDay;
    }

    public BigDecimal getTotalPointsConsumed() {
        return totalPointsConsumed;
    }

    public void setTotalPointsConsumed(BigDecimal totalPointsConsumed) {
        this.totalPointsConsumed = totalPointsConsumed;
    }

    public Instant getLastExecutionTime() {
        return lastExecutionTime;
    }

    public void setLastExecutionTime(Instant lastExecutionTime) {
        this.lastExecutionTime = lastExecutionTime;
    }

    public Instant getNextExecutionTime() {
        return nextExecutionTime;
    }

    public void setNextExecutionTime(Instant nextExecutionTime) {
        this.nextExecutionTime = nextExecutionTime;
    }

    public Integer getExecutionCount() {
        return executionCount;
    }

    public void setExecutionCount(Integer executionCount) {
        this.executionCount = executionCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(Instant pausedAt) {
        this.pausedAt = pausedAt;
    }

    public Instant getStoppedAt() {
        return stoppedAt;
    }

    public void setStoppedAt(Instant stoppedAt) {
        this.stoppedAt = stoppedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<BotIndicatorCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<BotIndicatorCondition> conditions) {
        this.conditions = conditions;
    }

    public List<BotTrade> getTrades() {
        return trades;
    }

    public void setTrades(List<BotTrade> trades) {
        this.trades = trades;
    }

    public List<BotPosition> getPositions() {
        return positions;
    }

    public void setPositions(List<BotPosition> positions) {
        this.positions = positions;
    }

    public TradingMode getTradingMode() { return tradingMode; }
    public void setTradingMode(TradingMode tradingMode) { this.tradingMode = tradingMode; }

    public boolean isVirtualCredit() { return virtualCredit; }
    public void setVirtualCredit(boolean virtualCredit) { this.virtualCredit = virtualCredit; }
}