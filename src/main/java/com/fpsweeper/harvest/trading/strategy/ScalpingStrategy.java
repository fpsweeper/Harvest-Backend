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
public class ScalpingStrategy implements TradingStrategy {

    private static final Logger log = LoggerFactory.getLogger(ScalpingStrategy.class);

    private static final BigDecimal DEFAULT_QUICK_PROFIT = new BigDecimal("0.8");
    private static final BigDecimal DEFAULT_TIGHT_STOP   = new BigDecimal("0.5");
    // Default: use 80% of balance per scalp trade
    private static final BigDecimal DEFAULT_POSITION_PCT = new BigDecimal("80");

    @Autowired private IndicatorService indicatorService;
    @Autowired private ConditionEvaluator conditionEvaluator;
    @Autowired private BotIndicatorConditionRepository conditionRepository;
    @Autowired private BotPositionRepository positionRepository;

    @Override
    public TradeSignal evaluate(TradingBot bot) {
        log.info("⚡ Evaluating Scalping for bot: {}", bot.getName());

        try {
            Map<String, BigDecimal> indicators = indicatorService.calculateIndicators(
                    bot.getTradingPair(), bot.getTimeframe());

            if (indicators.isEmpty()) return TradeSignal.hold("No indicator data available");

            BigDecimal currentPrice  = indicators.get("CLOSE_PRICE");
            BigDecimal rsi           = indicators.getOrDefault("RSI_14", BigDecimal.valueOf(50));
            BigDecimal macdHistogram = indicators.getOrDefault("MACD_HISTOGRAM", BigDecimal.ZERO);

            log.info("💹 price: ${} RSI: {} MACD-H: {}", currentPrice, rsi, macdHistogram);

            Map<String, Object> config = bot.getConfiguration();
            BigDecimal quickProfit     = getConfigValue(config, "quick_profit_target", DEFAULT_QUICK_PROFIT);
            BigDecimal tightStop       = getConfigValue(config, "tight_stop_loss", DEFAULT_TIGHT_STOP);

            // ── Exit open positions first ──────────────────────────────────────
            List<BotPosition> openPositions = positionRepository
                    .findByBotIdAndStatus(bot.getId(), PositionStatus.OPEN);

            if (!openPositions.isEmpty()) {
                for (BotPosition position : openPositions) {
                    BigDecimal pnlPct = currentPrice.subtract(position.getEntryPrice())
                            .divide(position.getEntryPrice(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));

                    if (pnlPct.compareTo(quickProfit) >= 0) {
                        log.info("💰 Quick profit hit! Profit: {}%", pnlPct);
                        return TradeSignal.sell(bot.getTradingPair(), position.getQuantity(),
                                String.format("Quick profit: %.2f%%", pnlPct), indicators);
                    }

                    if (pnlPct.compareTo(tightStop.negate()) <= 0) {
                        log.warn("🛑 Tight stop hit! Loss: {}%", pnlPct);
                        return TradeSignal.sell(bot.getTradingPair(), position.getQuantity(),
                                String.format("Tight stop: %.2f%%", pnlPct), indicators);
                    }

                    List<BotIndicatorCondition> exitConds = conditionRepository
                            .findByBotIdAndConditionTypeOrderByConditionOrder(bot.getId(), ConditionType.EXIT);
                    if (!exitConds.isEmpty() && conditionEvaluator.evaluateConditions(
                            exitConds, indicators, bot.getTradingPair(), bot.getTimeframe())) {
                        return TradeSignal.sell(bot.getTradingPair(), position.getQuantity(),
                                "Scalping exit conditions met", indicators);
                    }
                }
                return TradeSignal.hold("Waiting for profit target or stop loss");
            }

            // ── Entry ──────────────────────────────────────────────────────────
            List<BotIndicatorCondition> entryConds = conditionRepository
                    .findByBotIdAndConditionTypeOrderByConditionOrder(bot.getId(), ConditionType.ENTRY);

            boolean shouldEnter = entryConds.isEmpty()
                    ? isScalpingOpportunity(rsi, macdHistogram, indicators)
                    : conditionEvaluator.evaluateConditions(
                    entryConds, indicators, bot.getTradingPair(), bot.getTimeframe());

            if (shouldEnter) {
                BigDecimal size = calculatePositionSize(bot, currentPrice);
                if (size.compareTo(BigDecimal.ZERO) > 0) {
                    String reason = String.format("Scalping entry — RSI: %.2f, MACD-H: %.4f", rsi, macdHistogram);
                    return TradeSignal.buy(bot.getTradingPair(), size, reason, indicators);
                }
                return TradeSignal.hold("Insufficient balance for scalping entry");
            }

            return TradeSignal.hold("No scalping opportunity");

        } catch (Exception e) {
            log.error("❌ Error evaluating Scalping: {}", e.getMessage(), e);
            return TradeSignal.hold("Error: " + e.getMessage());
        }
    }

    private boolean isScalpingOpportunity(BigDecimal rsi, BigDecimal macdHistogram,
                                          Map<String, BigDecimal> indicators) {
        boolean rsiOk  = rsi.compareTo(BigDecimal.valueOf(30)) >= 0
                && rsi.compareTo(BigDecimal.valueOf(45)) <= 0;
        boolean macdOk = macdHistogram.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal ma20   = indicators.getOrDefault("MA_20", BigDecimal.ZERO);
        BigDecimal price  = indicators.get("CLOSE_PRICE");
        boolean priceOk   = ma20.compareTo(BigDecimal.ZERO) > 0
                && price.compareTo(ma20.multiply(new BigDecimal("1.01"))) <= 0;
        return rsiOk && macdOk && priceOk;
    }

    /**
     * Use bot's maxPositionSizePercentage if set, otherwise default to 80%.
     * Applies same normalization fix as DCAStrategy.
     */
    private BigDecimal calculatePositionSize(TradingBot bot, BigDecimal currentPrice) {
        BigDecimal balance = bot.getCurrentBalance();
        BigDecimal rawPct  = bot.getMaxPositionSizePercentage() != null
                ? bot.getMaxPositionSizePercentage()
                : DEFAULT_POSITION_PCT;

        // Normalize: if stored as decimal fraction (e.g. 0.80 instead of 80)
        BigDecimal pct = rawPct.compareTo(BigDecimal.ONE) < 0
                ? rawPct.multiply(BigDecimal.valueOf(100))
                : rawPct;

        BigDecimal positionValue = balance
                .multiply(pct)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

        BigDecimal qty = currentPrice.compareTo(BigDecimal.ZERO) > 0
                ? positionValue.divide(currentPrice, 8, RoundingMode.HALF_DOWN)
                : BigDecimal.ZERO;

        log.info("📐 Scalping size | balance: {} | rawPct: {} | normalizedPct: {} | positionValue: {} | qty: {}",
                balance, rawPct, pct, positionValue, qty);

        return qty;
    }

    @SuppressWarnings("unchecked")
    private <T> T getConfigValue(Map<String, Object> config, String key, T defaultValue) {
        if (config == null || !config.containsKey(key)) return defaultValue;
        Object value = config.get(key);
        if (value instanceof Number && defaultValue instanceof BigDecimal)
            return (T) new BigDecimal(value.toString());
        return defaultValue;
    }

    @Override
    public String getStrategyName() { return "Scalping"; }
}