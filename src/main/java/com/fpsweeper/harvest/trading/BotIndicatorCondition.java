package com.fpsweeper.harvest.trading;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bot_indicator_conditions")
public class BotIndicatorCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bot_id", nullable = false)
    private UUID botId;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 10)
    private ConditionType conditionType;

    @Column(name = "indicator_name", nullable = false, length = 50)
    private String indicatorName;

    @Column(name = "indicator_period")
    private Integer indicatorPeriod;

    @Column(nullable = false, length = 20)
    private String operator;

    @Column(name = "comparison_value", precision = 18, scale = 8)
    private BigDecimal comparisonValue;

    @Column(name = "comparison_indicator", length = 50)
    private String comparisonIndicator;

    @Column(name = "comparison_indicator_period")
    private Integer comparisonIndicatorPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "logical_operator", length = 5)
    private LogicalOperator logicalOperator = LogicalOperator.AND;

    @Column(name = "condition_order", nullable = false)
    private Integer conditionOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Constructors
    public BotIndicatorCondition() {
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

    public ConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ConditionType conditionType) {
        this.conditionType = conditionType;
    }

    public String getIndicatorName() {
        return indicatorName;
    }

    public void setIndicatorName(String indicatorName) {
        this.indicatorName = indicatorName;
    }

    public Integer getIndicatorPeriod() {
        return indicatorPeriod;
    }

    public void setIndicatorPeriod(Integer indicatorPeriod) {
        this.indicatorPeriod = indicatorPeriod;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public BigDecimal getComparisonValue() {
        return comparisonValue;
    }

    public void setComparisonValue(BigDecimal comparisonValue) {
        this.comparisonValue = comparisonValue;
    }

    public String getComparisonIndicator() {
        return comparisonIndicator;
    }

    public void setComparisonIndicator(String comparisonIndicator) {
        this.comparisonIndicator = comparisonIndicator;
    }

    public Integer getComparisonIndicatorPeriod() {
        return comparisonIndicatorPeriod;
    }

    public void setComparisonIndicatorPeriod(Integer comparisonIndicatorPeriod) {
        this.comparisonIndicatorPeriod = comparisonIndicatorPeriod;
    }

    public LogicalOperator getLogicalOperator() {
        return logicalOperator;
    }

    public void setLogicalOperator(LogicalOperator logicalOperator) {
        this.logicalOperator = logicalOperator;
    }

    public Integer getConditionOrder() {
        return conditionOrder;
    }

    public void setConditionOrder(Integer conditionOrder) {
        this.conditionOrder = conditionOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}