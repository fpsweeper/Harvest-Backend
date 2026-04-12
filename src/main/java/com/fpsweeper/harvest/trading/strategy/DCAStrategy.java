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
public class DCAStrategy implements TradingStrategy {

    private static final Logger log = LoggerFactory.getLogger(DCAStrategy.class);

    @Autowired
    private IndicatorService indicatorService;

    @Autowired
    private ConditionEvaluator conditionEvaluator;

    @Autowired
    private BotIndicatorConditionRepository conditionRepository;

    @Autowired
    private BotPositionRepository positionRepository;

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


            // Check entry conditions
            List<BotIndicatorCondition> entryConditions = conditionRepository
                    .findByBotIdAndConditionTypeOrderByConditionOrder(bot.getId(), ConditionType.ENTRY);

            if (!entryConditions.isEmpty()) {
                boolean shouldBuy = conditionEvaluator.evaluateConditions(entryConditions, indicators);

                if (shouldBuy) {
                    // Calculate DCA position size
                    BigDecimal positionSize = calculateDCAPositionSize(bot, currentPrice);

                    if (positionSize.compareTo(BigDecimal.ZERO) > 0) {
                        String reason = String.format("Entry conditions met - RSI: %.2f, MACD: %.4f",
                                indicators.getOrDefault("RSI_14", BigDecimal.ZERO),
                                indicators.getOrDefault("MACD", BigDecimal.ZERO));

                        return TradeSignal.buy(bot.getTradingPair(), positionSize, reason);
                    } else {
                        return TradeSignal.hold("Insufficient balance for DCA buy");
                    }
                }
            }

            // Check exit conditions (if we have open positions)
            List<BotPosition> openPositions = positionRepository.findByBotIdAndStatus(
                    bot.getId(),
                    PositionStatus.OPEN
            );

            if (!openPositions.isEmpty()) {
                List<BotIndicatorCondition> exitConditions = conditionRepository
                        .findByBotIdAndConditionTypeOrderByConditionOrder(bot.getId(), ConditionType.EXIT);

                if (!exitConditions.isEmpty()) {
                    boolean shouldSell = conditionEvaluator.evaluateConditions(exitConditions, indicators);

                    if (shouldSell) {
                        // Calculate total position to sell
                        BigDecimal totalPosition = openPositions.stream()
                                .map(BotPosition::getQuantity)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        String reason = String.format("Exit conditions met - RSI: %.2f, Profit target reached",
                                indicators.getOrDefault("RSI_14", BigDecimal.ZERO));

                        return TradeSignal.sell(bot.getTradingPair(), totalPosition, reason);
                    }
                }

                // Check stop loss / take profit
                TradeSignal stopLossSignal = checkStopLossTakeProfit(bot, openPositions, currentPrice);
                if (stopLossSignal.shouldTrade()) {
                    return stopLossSignal;
                }
            }

            return TradeSignal.hold("No conditions met");

        } catch (Exception e) {
            log.error("❌ Error evaluating DCA strategy: {}", e.getMessage(), e);
            return TradeSignal.hold("Error: " + e.getMessage());
        }
    }

    /**
     * Calculate DCA position size (fixed percentage of balance)
     */
    private BigDecimal calculateDCAPositionSize(TradingBot bot, BigDecimal currentPrice) {
        BigDecimal availableBalance = bot.getCurrentBalance();
        BigDecimal maxPositionPercentage = bot.getMaxPositionSizePercentage();

        // DCA: Use fixed percentage of balance for each buy
        BigDecimal positionValue = availableBalance
                .multiply(maxPositionPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Convert to quantity
        if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            return positionValue.divide(currentPrice, 8, RoundingMode.HALF_DOWN);
        }

        return BigDecimal.ZERO;
    }

    /**
     * Check stop loss and take profit
     */
    private TradeSignal checkStopLossTakeProfit(TradingBot bot, List<BotPosition> positions, BigDecimal currentPrice) {
        for (BotPosition position : positions) {
            BigDecimal entryPrice = position.getEntryPrice();

            // Calculate current P&L percentage
            BigDecimal pnlPercentage = currentPrice.subtract(entryPrice)
                    .divide(entryPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            // Check stop loss
            if (bot.getStopLossPercentage() != null) {
                BigDecimal stopLoss = bot.getStopLossPercentage().negate();
                if (pnlPercentage.compareTo(stopLoss) <= 0) {
                    log.warn("🛑 Stop loss triggered! P&L: {}%", pnlPercentage);

                    BigDecimal totalPosition = positions.stream()
                            .map(BotPosition::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return TradeSignal.sell(bot.getTradingPair(), totalPosition,
                            String.format("Stop loss triggered at %.2f%%", pnlPercentage));
                }
            }

            // Check take profit
            if (bot.getTakeProfitPercentage() != null) {
                if (pnlPercentage.compareTo(bot.getTakeProfitPercentage()) >= 0) {
                    log.info("🎯 Take profit triggered! P&L: {}%", pnlPercentage);

                    BigDecimal totalPosition = positions.stream()
                            .map(BotPosition::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return TradeSignal.sell(bot.getTradingPair(), totalPosition,
                            String.format("Take profit triggered at %.2f%%", pnlPercentage));
                }
            }
        }

        return TradeSignal.hold("Stop loss / take profit not triggered");
    }

    @Override
    public String getStrategyName() {
        return "DCA (Dollar Cost Averaging)";
    }
}