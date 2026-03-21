package com.fpsweeper.harvest.trading.service;

import com.fpsweeper.harvest.trading.BotIndicatorCondition;
import com.fpsweeper.harvest.trading.LogicalOperator;
import com.fpsweeper.harvest.trading.MarketDataCache;
import com.fpsweeper.harvest.trading.MarketDataCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class ConditionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(ConditionEvaluator.class);

    @Autowired
    private MarketDataCacheRepository marketDataCacheRepository;

    /**
     * Evaluate a list of conditions against current indicator values.
     * For crosses_above/crosses_below, also needs symbol+timeframe to fetch
     * the previous candle's indicator values for comparison.
     */
    public boolean evaluateConditions(
            List<BotIndicatorCondition> conditions,
            Map<String, BigDecimal> indicators
    ) {
        return evaluateConditions(conditions, indicators, null, null);
    }

    /**
     * Full evaluation with symbol+timeframe for cross detection.
     */
    public boolean evaluateConditions(
            List<BotIndicatorCondition> conditions,
            Map<String, BigDecimal> indicators,
            String symbol,
            String timeframe
    ) {
        if (conditions.isEmpty()) {
            log.warn("⚠️ No conditions to evaluate");
            return false;
        }

        log.debug("🔍 Evaluating {} conditions", conditions.size());

        // Lazy-load previous candle only if a cross condition exists
        Map<String, BigDecimal> previousIndicators = null;
        boolean hasCrossCondition = conditions.stream()
                .anyMatch(c -> c.getOperator().contains("crosses"));

        if (hasCrossCondition && symbol != null && timeframe != null) {
            previousIndicators = fetchPreviousCandleIndicators(symbol, timeframe);
        }

        boolean result = true;
        boolean isFirst = true;

        for (BotIndicatorCondition condition : conditions) {
            boolean conditionMet = evaluateSingleCondition(condition, indicators, previousIndicators);

            log.debug("   {} {} {} = {}",
                    condition.getIndicatorName(),
                    condition.getOperator(),
                    condition.getComparisonValue(),
                    conditionMet ? "✅" : "❌");

            if (isFirst) {
                result = conditionMet;
                isFirst = false;
            } else {
                if (condition.getLogicalOperator() == LogicalOperator.AND) {
                    result = result && conditionMet;
                } else if (condition.getLogicalOperator() == LogicalOperator.OR) {
                    result = result || conditionMet;
                }
            }
        }

        log.info("🎯 Condition result: {}", result ? "✅ MET" : "❌ NOT MET");
        return result;
    }

    private boolean evaluateSingleCondition(
            BotIndicatorCondition condition,
            Map<String, BigDecimal> current,
            Map<String, BigDecimal> previous
    ) {
        String indicatorName = condition.getIndicatorName();
        BigDecimal currentValue = current.get(indicatorName);

        if (currentValue == null) {
            log.error("❌ Indicator {} not found in calculated indicators", indicatorName);
            return false;
        }

        BigDecimal comparisonValue = condition.getComparisonValue();
        String operator = condition.getOperator().toLowerCase();

        switch (operator) {
            case "<":
            case "less_than":
                return currentValue.compareTo(comparisonValue) < 0;

            case ">":
            case "greater_than":
                return currentValue.compareTo(comparisonValue) > 0;

            case "<=":
            case "less_than_or_equal":
                return currentValue.compareTo(comparisonValue) <= 0;

            case ">=":
            case "greater_than_or_equal":
                return currentValue.compareTo(comparisonValue) >= 0;

            case "=":
            case "==":
            case "equals":
                return currentValue.compareTo(comparisonValue) == 0;

            case "!=":
                return currentValue.compareTo(comparisonValue) != 0;

            case "crosses_above":
                // Current candle is above threshold, previous candle was below
                // e.g. "MACD crosses_above 0" = MACD just turned positive
                if (previous == null) {
                    log.warn("⚠️ crosses_above: no previous candle data available, returning false");
                    return false;
                }
                BigDecimal prevValueAbove = previous.get(indicatorName);
                if (prevValueAbove == null) {
                    log.warn("⚠️ crosses_above: {} not in previous candle indicators", indicatorName);
                    return false;
                }
                // Current > threshold AND previous <= threshold
                return currentValue.compareTo(comparisonValue) > 0
                        && prevValueAbove.compareTo(comparisonValue) <= 0;

            case "crosses_below":
                // Current candle is below threshold, previous candle was above
                // e.g. "RSI crosses_below 70" = RSI just left overbought zone
                if (previous == null) {
                    log.warn("⚠️ crosses_below: no previous candle data available, returning false");
                    return false;
                }
                BigDecimal prevValueBelow = previous.get(indicatorName);
                if (prevValueBelow == null) {
                    log.warn("⚠️ crosses_below: {} not in previous candle indicators", indicatorName);
                    return false;
                }
                // Current < threshold AND previous >= threshold
                return currentValue.compareTo(comparisonValue) < 0
                        && prevValueBelow.compareTo(comparisonValue) >= 0;

            default:
                log.error("❌ Unknown operator: {}", operator);
                return false;
        }
    }

    /**
     * Fetch the second-most-recent candle's cached indicators.
     * Used for cross detection — compare current vs previous values.
     */
    private Map<String, BigDecimal> fetchPreviousCandleIndicators(String symbol, String timeframe) {
        try {
            List<MarketDataCache> candles = marketDataCacheRepository
                    .findBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe);

            // Index 0 = most recent (current), index 1 = previous candle
            if (candles.size() < 2) {
                log.warn("⚠️ Not enough candles for cross detection ({} candles)", candles.size());
                return null;
            }

            MarketDataCache prev = candles.get(1);

            // Build indicator map from cached values
            Map<String, BigDecimal> prevIndicators = new java.util.HashMap<>();
            if (prev.getRsi14() != null)        prevIndicators.put("RSI_14", prev.getRsi14());
            if (prev.getRsi7() != null)         prevIndicators.put("RSI_7", prev.getRsi7());
            if (prev.getMacd() != null)         prevIndicators.put("MACD", prev.getMacd());
            if (prev.getMacdSignal() != null)   prevIndicators.put("MACD_SIGNAL", prev.getMacdSignal());
            if (prev.getMacdHistogram() != null) prevIndicators.put("MACD_HISTOGRAM", prev.getMacdHistogram());
            if (prev.getMa20() != null)         prevIndicators.put("MA_20", prev.getMa20());
            if (prev.getMa50() != null)         prevIndicators.put("MA_50", prev.getMa50());
            if (prev.getMa100() != null)        prevIndicators.put("MA_100", prev.getMa100());
            if (prev.getMa200() != null)        prevIndicators.put("MA_200", prev.getMa200());
            if (prev.getEma12() != null)        prevIndicators.put("EMA_12", prev.getEma12());
            if (prev.getEma26() != null)        prevIndicators.put("EMA_26", prev.getEma26());
            if (prev.getBbUpper() != null)      prevIndicators.put("BB_UPPER", prev.getBbUpper());
            if (prev.getBbMiddle() != null)     prevIndicators.put("BB_MIDDLE", prev.getBbMiddle());
            if (prev.getBbLower() != null)      prevIndicators.put("BB_LOWER", prev.getBbLower());
            if (prev.getClosePrice() != null)   prevIndicators.put("CLOSE_PRICE", prev.getClosePrice());

            return prevIndicators;

        } catch (Exception e) {
            log.error("❌ Error fetching previous candle indicators: {}", e.getMessage());
            return null;
        }
    }
}