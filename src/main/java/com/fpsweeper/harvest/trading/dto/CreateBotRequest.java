package com.fpsweeper.harvest.trading.dto;

import com.fpsweeper.harvest.trading.StrategyType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class CreateBotRequest {

    @NotBlank(message = "Bot name is required")
    @Size(min = 3, max = 100, message = "Bot name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;

    @NotNull(message = "Strategy type is required")
    private StrategyType strategyType;

    @NotBlank(message = "Trading pair is required")
    @Pattern(regexp = "^[A-Z]{3,10}USDT$", message = "Trading pair must end with USDT (e.g., BTCUSDT)")
    private String tradingPair;

    @NotBlank(message = "Timeframe is required")
    @Pattern(regexp = "^(\\d+[mhd]|1w)$", message = "Valid timeframes: 1m, 5m, 15m, 30m, 1h, 4h, 1d, 1w")
    private String timeframe;

    @NotNull(message = "Initial balance is required")
    @DecimalMin(value = "100.0", message = "Minimum initial balance is $100")
    @DecimalMax(value = "1000000.0", message = "Maximum initial balance is $1,000,000")
    private BigDecimal initialBalance;

    @DecimalMin(value = "0.1", message = "Stop loss must be at least 0.1%")
    @DecimalMax(value = "50.0", message = "Stop loss cannot exceed 50%")
    private BigDecimal stopLossPercentage;

    @DecimalMin(value = "0.1", message = "Take profit must be at least 0.1%")
    @DecimalMax(value = "1000.0", message = "Take profit cannot exceed 1000%")
    private BigDecimal takeProfitPercentage;

    @NotNull(message = "Max position size percentage is required")
    @DecimalMin(value = "1.0", message = "Min position size is 1%")
    @DecimalMax(value = "100.0", message = "Max position size is 100%")
    private BigDecimal maxPositionSizePercentage;

    // Entry conditions
    private List<IndicatorConditionRequest> entryConditions;

    // Exit conditions
    private List<IndicatorConditionRequest> exitConditions;

    // Strategy-specific configuration
    private Map<String, Object> configuration;

    // Getters and Setters
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

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(BigDecimal initialBalance) {
        this.initialBalance = initialBalance;
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

    public List<IndicatorConditionRequest> getEntryConditions() {
        return entryConditions;
    }

    public void setEntryConditions(List<IndicatorConditionRequest> entryConditions) {
        this.entryConditions = entryConditions;
    }

    public List<IndicatorConditionRequest> getExitConditions() {
        return exitConditions;
    }

    public void setExitConditions(List<IndicatorConditionRequest> exitConditions) {
        this.exitConditions = exitConditions;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }
}