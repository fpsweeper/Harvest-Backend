package com.fpsweeper.harvest.trading.dto;

import com.fpsweeper.harvest.trading.LogicalOperator;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class IndicatorConditionRequest {

    @NotBlank(message = "Indicator name is required")
    private String indicatorName;

    private Integer indicatorPeriod;

    @NotBlank(message = "Operator is required")
    @Pattern(regexp = "^(<|>|<=|>=|=|==|crosses_above|crosses_below|less_than|greater_than)$",
            message = "Invalid operator")
    private String operator;

    @NotNull(message = "Comparison value is required")
    private BigDecimal comparisonValue;

    private LogicalOperator logicalOperator = LogicalOperator.AND;

    private Integer conditionOrder = 0;

    // Getters and Setters
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
}