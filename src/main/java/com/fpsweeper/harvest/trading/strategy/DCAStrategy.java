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

    @Autowired private IndicatorService indicatorService;
    @Autowired private ConditionEvaluator conditionEvaluator;
    @Autowired private BotIndicatorConditionRepository conditionRepository;
    @Autowired private BotPositionRepository positionRepository;

    @Override
    public TradeSignal evaluate(TradingBot bot) {
        log.info("💰 Evaluating DCA strategy for bot: {}", bot.getName());

        try {
            Map<String, BigDecimal> indicators = indicatorService.calculateIndicators(
                    bot.getTradingPair(), bot.getTimeframe());

            if (indicators.isEmpty()) return TradeSignal.hold("No indicator data available");

            BigDecimal currentPrice = indicators.get("CLOSE_PRICE");
            log.info("📊 Current price: ${}", currentPrice);

            // ── Entry ──────────────────────────────────────────────────────────
            List<BotIndicatorCondition> entryConditions = conditionRepository
                    .findByBotIdAndConditionTypeOrderByConditionOrder(bot.getId(), ConditionType.ENTRY);

            if (!entryConditions.isEmpty()) {
                boolean shouldBuy = conditionEvaluator.evaluateConditions(
                        entryConditions, indicators, bot.getTradingPair(), bot.getTimeframe());

                if (shouldBuy) {
                    BigDecimal positionSize = calculatePositionSize(bot, currentPrice);
                    if (positionSize.compareTo(BigDecimal.ZERO) > 0) {
                        String reason = String.format("Entry conditions met — RSI: %.2f, MACD: %.4f",
                                indicators.getOrDefault("RSI_14", BigDecimal.ZERO),
                                indicators.getOrDefault("MACD", BigDecimal.ZERO));
                        return TradeSignal.buy(bot.getTradingPair(), positionSize, reason, indicators);
                    }
                    return TradeSignal.hold("Insufficient balance for DCA buy");
                }
            }

            // ── Exit ───────────────────────────────────────────────────────────
            List<BotPosition> openPositions = positionRepository
                    .findByBotIdAndStatus(bot.getId(), PositionStatus.OPEN);

            if (!openPositions.isEmpty()) {
                List<BotIndicatorCondition> exitConditions = conditionRepository
                        .findByBotIdAndConditionTypeOrderByConditionOrder(bot.getId(), ConditionType.EXIT);

                if (!exitConditions.isEmpty()) {
                    boolean shouldSell = conditionEvaluator.evaluateConditions(
                            exitConditions, indicators, bot.getTradingPair(), bot.getTimeframe());

                    if (shouldSell) {
                        BigDecimal totalPosition = openPositions.stream()
                                .map(BotPosition::getQuantity)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        String reason = String.format("Exit conditions met — RSI: %.2f",
                                indicators.getOrDefault("RSI_14", BigDecimal.ZERO));
                        return TradeSignal.sell(bot.getTradingPair(), totalPosition, reason, indicators);
                    }
                }

                TradeSignal slSignal = checkStopLossTakeProfit(bot, openPositions, currentPrice, indicators);
                if (slSignal.shouldTrade()) return slSignal;
            }

            return TradeSignal.hold("No conditions met");

        } catch (Exception e) {
            log.error("❌ Error evaluating DCA strategy: {}", e.getMessage(), e);
            return TradeSignal.hold("Error: " + e.getMessage());
        }
    }

    /**
     * Calculate position size in base currency (e.g. BTC).
     *
     * FIX: maxPositionSizePercentage may be stored as a decimal fraction
     * (0.30 instead of 30) depending on Hibernate column mapping.
     * We normalize it: if value < 1 we assume it's already a fraction and
     * multiply by 100 to get the percentage.
     *
     * Diagnostic log always emits so we can verify in logs immediately.
     */
    private BigDecimal calculatePositionSize(TradingBot bot, BigDecimal currentPrice) {
        BigDecimal balance = bot.getCurrentBalance();
        BigDecimal rawPct  = bot.getMaxPositionSizePercentage();

        // Normalize percentage
        BigDecimal pct = rawPct.compareTo(BigDecimal.ONE) < 0
                ? rawPct.multiply(BigDecimal.valueOf(100))
                : rawPct;

        BigDecimal positionValue = balance
                .multiply(pct)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        BigDecimal qty = currentPrice.compareTo(BigDecimal.ZERO) > 0
                ? positionValue.divide(currentPrice, 8, RoundingMode.HALF_DOWN)
                : BigDecimal.ZERO;

        log.info("📐 DCA size | balance: {} | rawPct: {} | normalizedPct: {} | positionValue: {} | qty: {}",
                balance, rawPct, pct, positionValue, qty);

        return qty;
    }

    private TradeSignal checkStopLossTakeProfit(TradingBot bot, List<BotPosition> positions,
                                                BigDecimal currentPrice,
                                                Map<String, BigDecimal> indicators) {
        for (BotPosition position : positions) {
            BigDecimal pnlPct = currentPrice.subtract(position.getEntryPrice())
                    .divide(position.getEntryPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            BigDecimal total = positions.stream()
                    .map(BotPosition::getQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (bot.getStopLossPercentage() != null
                    && pnlPct.compareTo(bot.getStopLossPercentage().negate()) <= 0) {
                log.warn("🛑 Stop loss triggered! P&L: {}%", pnlPct);
                return TradeSignal.sell(bot.getTradingPair(), total,
                        String.format("Stop loss at %.2f%%", pnlPct), indicators);
            }

            if (bot.getTakeProfitPercentage() != null
                    && pnlPct.compareTo(bot.getTakeProfitPercentage()) >= 0) {
                log.info("🎯 Take profit triggered! P&L: {}%", pnlPct);
                return TradeSignal.sell(bot.getTradingPair(), total,
                        String.format("Take profit at %.2f%%", pnlPct), indicators);
            }
        }
        return TradeSignal.hold("SL/TP not triggered");
    }

    @Override
    public String getStrategyName() { return "DCA (Dollar Cost Averaging)"; }
}