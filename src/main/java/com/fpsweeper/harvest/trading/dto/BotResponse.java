package com.fpsweeper.harvest.trading.dto;

import com.fpsweeper.harvest.trading.BotStatus;
import com.fpsweeper.harvest.trading.StrategyType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class BotResponse {

    private UUID id;
    private String name;
    private String description;
    private StrategyType strategyType;
    private String tradingPair;
    private String timeframe;
    private BotStatus status;

    private BigDecimal initialBalance;
    private BigDecimal currentBalance;
    private BigDecimal totalPnl;
    private BigDecimal totalPnlPercentage;

    private BigDecimal stopLossPercentage;
    private BigDecimal takeProfitPercentage;
    private BigDecimal maxPositionSizePercentage;

    private Integer totalTrades;
    private Integer openPositions;

    private Instant createdAt;
    private Instant startedAt;
    private Instant lastExecutionTime;
    private Instant nextExecutionTime;

    private Map<String, Object> configuration;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public BigDecimal getTotalPnl() {
        return totalPnl;
    }

    public void setTotalPnl(BigDecimal totalPnl) {
        this.totalPnl = totalPnl;
    }

    public BigDecimal getTotalPnlPercentage() {
        return totalPnlPercentage;
    }

    public void setTotalPnlPercentage(BigDecimal totalPnlPercentage) {
        this.totalPnlPercentage = totalPnlPercentage;
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

    public Integer getTotalTrades() {
        return totalTrades;
    }

    public void setTotalTrades(Integer totalTrades) {
        this.totalTrades = totalTrades;
    }

    public Integer getOpenPositions() {
        return openPositions;
    }

    public void setOpenPositions(Integer openPositions) {
        this.openPositions = openPositions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
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

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
}