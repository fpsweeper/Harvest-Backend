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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class GridStrategy implements TradingStrategy {

    private static final Logger log = LoggerFactory.getLogger(GridStrategy.class);

    private static final int        DEFAULT_GRID_LEVELS  = 5;
    private static final BigDecimal DEFAULT_GRID_SPACING = new BigDecimal("2.0");
    private static final BigDecimal PROFIT_PER_GRID      = new BigDecimal("2.5");

    @Autowired private IndicatorService indicatorService;
    @Autowired private ConditionEvaluator conditionEvaluator;
    @Autowired private BotIndicatorConditionRepository conditionRepository;
    @Autowired private BotPositionRepository positionRepository;
    @Autowired private BotTradeRepository tradeRepository;

    @Override
    public TradeSignal evaluate(TradingBot bot) {
        log.info("📊 Evaluating Grid for bot: {}", bot.getName());

        try {
            Map<String, BigDecimal> indicators = indicatorService.calculateIndicators(
                    bot.getTradingPair(), bot.getTimeframe());

            if (indicators.isEmpty()) return TradeSignal.hold("No indicator data available");

            BigDecimal currentPrice = indicators.get("CLOSE_PRICE");
            log.info("💵 Current price: ${}", currentPrice);

            Map<String, Object> config = bot.getConfiguration();
            int        gridLevels  = getConfigValue(config, "grid_levels",          DEFAULT_GRID_LEVELS);
            BigDecimal gridSpacing = getConfigValue(config, "grid_spacing_percent",  DEFAULT_GRID_SPACING);

            // ── Sell profitable positions ──────────────────────────────────────
            List<BotPosition> openPositions = positionRepository
                    .findByBotIdAndStatus(bot.getId(), PositionStatus.OPEN);

            for (BotPosition position : openPositions) {
                BigDecimal profitPct = currentPrice.subtract(position.getEntryPrice())
                        .divide(position.getEntryPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                if (profitPct.compareTo(PROFIT_PER_GRID) >= 0) {
                    log.info("✅ Grid sell triggered! Profit: {}%", profitPct);
                    return TradeSignal.sell(bot.getTradingPair(), position.getQuantity(),
                            String.format("Grid profit: %.2f%%", profitPct), indicators);
                }
            }

            // ── Entry conditions ──────────────────────────────────────────────
            List<BotIndicatorCondition> entryConds = conditionRepository
                    .findByBotIdAndConditionTypeOrderByConditionOrder(bot.getId(), ConditionType.ENTRY);

            boolean condsMet = entryConds.isEmpty() || conditionEvaluator.evaluateConditions(
                    entryConds, indicators, bot.getTradingPair(), bot.getTimeframe());

            if (condsMet) {
                List<BigDecimal> gridLevelsList = calculateGridLevels(currentPrice, gridLevels, gridSpacing);

                for (BigDecimal gridLevel : gridLevelsList) {
                    BigDecimal tolerance = gridLevel.multiply(new BigDecimal("0.005"));
                    if (currentPrice.compareTo(gridLevel.subtract(tolerance)) >= 0
                            && currentPrice.compareTo(gridLevel.add(tolerance)) <= 0
                            && !hasPositionAtPrice(bot.getId(), gridLevel, tolerance)) {

                        BigDecimal size = calculateGridSize(bot, currentPrice, gridLevels);
                        if (size.compareTo(BigDecimal.ZERO) > 0) {
                            log.info("📍 Grid buy at ${}", gridLevel);
                            return TradeSignal.buy(bot.getTradingPair(), size,
                                    String.format("Grid buy at $%.2f", gridLevel), indicators);
                        }
                    }
                }
            }

            // ── Stop loss ─────────────────────────────────────────────────────
            TradeSignal sl = checkStopLoss(bot, openPositions, currentPrice, indicators);
            if (sl.shouldTrade()) return sl;

            return TradeSignal.hold("No grid levels triggered");

        } catch (Exception e) {
            log.error("❌ Grid error: {}", e.getMessage(), e);
            return TradeSignal.hold("Error: " + e.getMessage());
        }
    }

    private List<BigDecimal> calculateGridLevels(BigDecimal price, int levels, BigDecimal spacing) {
        return IntStream.range(1, levels + 1).mapToObj(i -> {
            BigDecimal discount = spacing.multiply(BigDecimal.valueOf(i))
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return price.multiply(BigDecimal.ONE.subtract(discount));
        }).collect(Collectors.toList());
    }

    /**
     * Split balance equally across grid levels.
     * Applies same normalization fix — if balance-per-level is mistakenly tiny
     * the diagnostic log will show it immediately.
     */
    private BigDecimal calculateGridSize(TradingBot bot, BigDecimal price, int levels) {
        BigDecimal balance  = bot.getCurrentBalance();
        BigDecimal perLevel = balance.divide(BigDecimal.valueOf(levels), 8, RoundingMode.HALF_UP);

        BigDecimal qty = price.compareTo(BigDecimal.ZERO) > 0
                ? perLevel.divide(price, 8, RoundingMode.HALF_DOWN)
                : BigDecimal.ZERO;

        log.info("📐 Grid size | balance: {} | levels: {} | perLevel: {} | price: {} | qty: {}",
                balance, levels, perLevel, price, qty);

        return qty;
    }

    private boolean hasPositionAtPrice(UUID botId, BigDecimal target, BigDecimal tolerance) {
        return positionRepository.findByBotIdAndStatus(botId, PositionStatus.OPEN).stream()
                .anyMatch(p -> p.getEntryPrice().compareTo(target.subtract(tolerance)) >= 0
                        && p.getEntryPrice().compareTo(target.add(tolerance)) <= 0);
    }

    private TradeSignal checkStopLoss(TradingBot bot, List<BotPosition> positions,
                                      BigDecimal currentPrice, Map<String, BigDecimal> indicators) {
        if (bot.getStopLossPercentage() == null || positions.isEmpty())
            return TradeSignal.hold("No stop loss");

        for (BotPosition position : positions) {
            BigDecimal pnlPct = currentPrice.subtract(position.getEntryPrice())
                    .divide(position.getEntryPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            if (pnlPct.compareTo(bot.getStopLossPercentage().negate()) <= 0) {
                log.warn("🛑 Grid stop loss triggered! P&L: {}%", pnlPct);
                BigDecimal total = positions.stream()
                        .map(BotPosition::getQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return TradeSignal.sell(bot.getTradingPair(), total,
                        String.format("Stop loss at %.2f%%", pnlPct), indicators);
            }
        }
        return TradeSignal.hold("Stop loss not triggered");
    }

    @SuppressWarnings("unchecked")
    private <T> T getConfigValue(Map<String, Object> config, String key, T defaultValue) {
        if (config == null || !config.containsKey(key)) return defaultValue;
        Object value = config.get(key);
        if (value instanceof Number && defaultValue instanceof BigDecimal)
            return (T) new BigDecimal(value.toString());
        if (value instanceof Number && defaultValue instanceof Integer)
            return (T) Integer.valueOf(((Number) value).intValue());
        return defaultValue;
    }

    @Override
    public String getStrategyName() { return "Grid Trading"; }
}