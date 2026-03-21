package com.fpsweeper.harvest.trading.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class UpdateBotRequest {
    private String name;
    private String description;
    private String tradingPair;
    private String timeframe;
    private BigDecimal stopLossPercentage;
    private BigDecimal takeProfitPercentage;
    private BigDecimal maxPositionSizePercentage;
    private Map<String, Object> configuration;
    private List<IndicatorConditionRequest> entryConditions;
    private List<IndicatorConditionRequest> exitConditions;
    // getters + setters


    public UpdateBotRequest() {
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

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
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
}
