package com.fpsweeper.harvest.trading.strategy;

import com.fpsweeper.harvest.trading.*;
import com.fpsweeper.harvest.trading.service.ConditionEvaluator;
import com.fpsweeper.harvest.trading.service.IndicatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

@Service
public class GridStrategy implements TradingStrategy {

    private static final Logger log = LoggerFactory.getLogger(GridStrategy.class);

    // Grid configuration
    private static final int DEFAULT_GRID_LEVELS = 5;
    private static final BigDecimal DEFAULT_GRID_SPACING_PERCENT = new BigDecimal("2.0"); // 2% between levels
    private static final BigDecimal PROFIT_PER_GRID = new BigDecimal("2.5"); // 2.5% profit per grid

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private ConditionEvaluator conditionEvaluator;

    @Autowired
    private BotIndicatorConditionRepository conditionRepository;

    @Autowired
    private BotPositionRepository positionRepository;

    @Autowired
    private BotTradeRepository tradeRepository;

    @Override
    public TradeSignal evaluate(TradingBot bot) {


        try {
            // Calculate all indicators
            Map<String, BigDecimal> indicators = indicatorService.calculateIndicators(
                    bot.getTradingPair(),
                    bot.getTimeframe()
            );

            if (indicators.isEmpty()) {
                return TradeSignal.hold("No indicator data available");
            }

            BigDecimal currentPrice = indicators.get("CLOSE_PRICE");


            // Get grid configuration from bot config (or use defaults)
            Map<String, Object> config = bot.getConfiguration();
            int gridLevels = getConfigValue(config, "grid_levels", DEFAULT_GRID_LEVELS);
            BigDecimal gridSpacing = getConfigValue(config, "grid_spacing_percent", DEFAULT_GRID_SPACING_PERCENT);

            // Check if we should sell first (take profit on existing positions)
            List<BotPosition> openPositions = positionRepository.findByBotIdAndStatus(
                    bot.getId(),
                    PositionStatus.OPEN
            );

            for (BotPosition position : openPositions) {
                // Calculate profit percentage
                BigDecimal entryPrice = position.getEntryPrice();
                BigDecimal profitPercent = currentPrice.subtract(entryPrice)
                        .divide(entryPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                // Sell if profit target reached (grid profit target)
                if (profitPercent.compareTo(PROFIT_PER_GRID) >= 0) {
                    log.info("✅ Grid sell triggered! Entry: ${}, Current: ${}, Profit: {}%",
                            entryPrice, currentPrice, profitPercent);

                    return TradeSignal.sell(
                            bot.getTradingPair(),
                            position.getQuantity(),
                            String.format("Grid profit reached: %.2f%%", profitPercent)
                    );
                }
            }

            // Check entry conditions (optional - grid can work without indicators)
            List<BotIndicatorCondition> entryConditions = conditionRepository
                    .findByBotIdAndConditionTypeOrderByConditionOrder(bot.getId(), ConditionType.ENTRY);

            boolean indicatorConditionsMet = true;
            if (!entryConditions.isEmpty()) {
                indicatorConditionsMet = conditionEvaluator.evaluateConditions(entryConditions, indicators);
            }

            // Calculate grid levels and check if current price matches a buy level
            if (indicatorConditionsMet) {
                List<BigDecimal> gridBuyLevels = calculateGridBuyLevels(currentPrice, gridLevels, gridSpacing);

                // Check if current price is near a grid level (within 0.5%)
                for (BigDecimal gridLevel : gridBuyLevels) {
                    BigDecimal tolerance = gridLevel.multiply(new BigDecimal("0.005")); // 0.5% tolerance

                    if (currentPrice.compareTo(gridLevel.subtract(tolerance)) >= 0 &&
                            currentPrice.compareTo(gridLevel.add(tolerance)) <= 0) {

                        // Check if we already have a position at this level
                        if (!hasPositionAtPrice(bot.getId(), gridLevel, tolerance)) {
                            BigDecimal positionSize = calculateGridPositionSize(bot, currentPrice, gridLevels);

                            if (positionSize.compareTo(BigDecimal.ZERO) > 0) {


                                return TradeSignal.buy(
                                        bot.getTradingPair(),
                                        positionSize,
                                        String.format("Grid buy at $%.2f", gridLevel)
                                );
                            }
                        }
                    }
                }
            }

            // Check stop loss on all positions
            TradeSignal stopLossSignal = checkStopLoss(bot, openPositions, currentPrice);
            if (stopLossSignal.shouldTrade()) {
                return stopLossSignal;
            }

            return TradeSignal.hold("No grid levels triggered");

        } catch (Exception e) {
            log.error("❌ Error evaluating Grid strategy: {}", e.getMessage(), e);
            return TradeSignal.hold("Error: " + e.getMessage());
        }
    }

    /**
     * Calculate grid buy levels below current price
     */
    private List<BigDecimal> calculateGridBuyLevels(BigDecimal currentPrice, int levels, BigDecimal spacingPercent) {
        return java.util.stream.IntStream.range(1, levels + 1)
                .mapToObj(i -> {
                    BigDecimal discount = spacingPercent.multiply(BigDecimal.valueOf(i))
                            .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    return currentPrice.multiply(BigDecimal.ONE.subtract(discount));
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Calculate position size for each grid level
     */
    private BigDecimal calculateGridPositionSize(TradingBot bot, BigDecimal currentPrice, int gridLevels) {
        BigDecimal availableBalance = bot.getCurrentBalance();

        // Divide balance across grid levels
        BigDecimal balancePerLevel = availableBalance.divide(
                BigDecimal.valueOf(gridLevels),
                2,
                RoundingMode.HALF_UP
        );

        // Convert to quantity
        if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            return balancePerLevel.divide(currentPrice, 8, RoundingMode.HALF_DOWN);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Check if we already have a position at this price level
     */
    private boolean hasPositionAtPrice(java.util.UUID botId, BigDecimal targetPrice, BigDecimal tolerance) {
        List<BotPosition> positions = positionRepository.findByBotIdAndStatus(botId, PositionStatus.OPEN);

        for (BotPosition position : positions) {
            BigDecimal entryPrice = position.getEntryPrice();
            if (entryPrice.compareTo(targetPrice.subtract(tolerance)) >= 0 &&
                    entryPrice.compareTo(targetPrice.add(tolerance)) <= 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check stop loss
     */
    private TradeSignal checkStopLoss(TradingBot bot, List<BotPosition> positions, BigDecimal currentPrice) {
        if (bot.getStopLossPercentage() == null || positions.isEmpty()) {
            return TradeSignal.hold("No stop loss configured");
        }

        for (BotPosition position : positions) {
            BigDecimal entryPrice = position.getEntryPrice();
            BigDecimal pnlPercentage = currentPrice.subtract(entryPrice)
                    .divide(entryPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            BigDecimal stopLoss = bot.getStopLossPercentage().negate();
            if (pnlPercentage.compareTo(stopLoss) <= 0) {
                log.warn("🛑 Grid stop loss triggered! P&L: {}%", pnlPercentage);

                BigDecimal totalPosition = positions.stream()
                        .map(BotPosition::getQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                return TradeSignal.sell(bot.getTradingPair(), totalPosition,
                        String.format("Stop loss triggered at %.2f%%", pnlPercentage));
            }
        }

        return TradeSignal.hold("Stop loss not triggered");
    }

    /**
     * Get configuration value with default
     */
    private <T> T getConfigValue(Map<String, Object> config, String key, T defaultValue) {
        if (config == null || !config.containsKey(key)) {
            return defaultValue;
        }

        Object value = config.get(key);
        if (value instanceof Number && defaultValue instanceof BigDecimal) {
            return (T) new BigDecimal(value.toString());
        } else if (value instanceof Number && defaultValue instanceof Integer) {
            return (T) Integer.valueOf(((Number) value).intValue());
        }

        return defaultValue;
    }

    @Override
    public String getStrategyName() {
        return "Grid Trading";
    }
}