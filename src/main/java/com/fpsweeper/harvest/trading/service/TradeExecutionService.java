package com.fpsweeper.harvest.trading.service;

import com.fpsweeper.harvest.trading.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class TradeExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TradeExecutionService.class);

    private static final BigDecimal SLIPPAGE_PERCENT    = new BigDecimal("0.1");
    private static final BigDecimal TRADING_FEE_PERCENT = new BigDecimal("0.1");

    @Autowired private BotTradeRepository     tradeRepository;
    @Autowired private BotPositionRepository  positionRepository;
    @Autowired private TradingBotRepository   botRepository;
    @Autowired private MarketDataService      marketDataService;

    // ─── BUY ──────────────────────────────────────────────────────────────────

    /** Execute a BUY order — without indicator snapshot (e.g. stop-bot close) */
    @Transactional
    public BotTrade executeBuy(TradingBot bot, String symbol, BigDecimal quantity, String reason) {
        return executeBuy(bot, symbol, quantity, reason, null);
    }

    /**
     * Execute a BUY order with indicator snapshot.
     * The indicators map is stored as JSONB on the trade record so users can
     * later see exactly what RSI/MACD/price looked like when the signal fired.
     */
    @Transactional
    public BotTrade executeBuy(TradingBot bot, String symbol, BigDecimal quantity,
                               String reason, Map<String, BigDecimal> indicators) {
        log.info("🛒 BUY | bot: {} | symbol: {} | qty: {}", bot.getName(), symbol, quantity);

        try {
            BigDecimal marketPrice    = marketDataService.getCurrentPrice(symbol);
            BigDecimal slippageAmount = marketPrice.multiply(SLIPPAGE_PERCENT)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal executionPrice = marketPrice.add(slippageAmount);
            BigDecimal totalValue     = quantity.multiply(executionPrice);
            BigDecimal fees           = totalValue.multiply(TRADING_FEE_PERCENT)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal totalCost      = totalValue.add(fees);

            if (bot.getCurrentBalance().compareTo(totalCost) < 0) {
                log.error("❌ Insufficient balance! Required: ${}, Available: ${}", totalCost, bot.getCurrentBalance());
                throw new RuntimeException("Insufficient balance for trade");
            }

            BotTrade trade = new BotTrade();
            trade.setBotId(bot.getId());
            trade.setTradeType(TradeType.BUY);
            trade.setSymbol(symbol);
            trade.setAmount(quantity);
            trade.setPrice(executionPrice);
            trade.setTotalValue(totalValue);
            trade.setFees(fees);
            trade.setSlippage(slippageAmount);
            trade.setStatus(TradeStatus.FILLED);
            trade.setIsSimulation(true);
            trade.setExecutedAt(Instant.now());

            // ✅ Store indicator snapshot
            if (indicators != null && !indicators.isEmpty()) {
                trade.setIndicatorValues(buildIndicatorSnapshot(indicators));
            }

            BotTrade savedTrade = tradeRepository.save(trade);

            // Create or average-down position
            Optional<BotPosition> existing = positionRepository
                    .findByBotIdAndSymbolAndStatus(bot.getId(), symbol, PositionStatus.OPEN);

            BotPosition position;
            if (existing.isPresent()) {
                position = existing.get();
                BigDecimal newQty   = position.getQuantity().add(quantity);
                BigDecimal newValue = position.getEntryValue().add(totalValue);
                BigDecimal newAvg   = newValue.divide(newQty, 2, RoundingMode.HALF_UP);
                position.setQuantity(newQty);
                position.setEntryValue(newValue);
                position.setEntryPrice(newAvg);
                position.setUpdatedAt(Instant.now());
                log.info("📊 Averaged position | new avg: ${}", newAvg);
            } else {
                position = new BotPosition();
                position.setBotId(bot.getId());
                position.setSymbol(symbol);
                position.setQuantity(quantity);
                position.setEntryPrice(executionPrice);
                position.setEntryValue(totalValue);
                position.setEntryTradeId(savedTrade.getId());
                position.setStatus(PositionStatus.OPEN);
                position.setOpenedAt(Instant.now());
                log.info("📍 New position | entry: ${}", executionPrice);
            }

            positionRepository.save(position);

            bot.setCurrentBalance(bot.getCurrentBalance().subtract(totalCost));
            bot.setLastExecutionTime(Instant.now());
            bot.setExecutionCount(bot.getExecutionCount() + 1);
            botRepository.save(bot);

            log.info("✅ BUY done | price: ${} | total: ${} | fees: ${} | balance: ${}",
                    executionPrice, totalValue, fees, bot.getCurrentBalance());

            return savedTrade;

        } catch (Exception e) {
            log.error("❌ BUY failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute buy order: " + e.getMessage(), e);
        }
    }

    // ─── SELL ─────────────────────────────────────────────────────────────────

    /** Execute a SELL order — without indicator snapshot (e.g. stop-bot close) */
    @Transactional
    public BotTrade executeSell(TradingBot bot, String symbol, BigDecimal quantity, String reason) {
        return executeSell(bot, symbol, quantity, reason, null);
    }

    /**
     * Execute a SELL order with indicator snapshot.
     */
    @Transactional
    public BotTrade executeSell(TradingBot bot, String symbol, BigDecimal quantity,
                                String reason, Map<String, BigDecimal> indicators) {
        log.info("💰 SELL | bot: {} | symbol: {} | qty: {}", bot.getName(), symbol, quantity);

        try {
            BigDecimal marketPrice    = marketDataService.getCurrentPrice(symbol);
            BigDecimal slippageAmount = marketPrice.multiply(SLIPPAGE_PERCENT)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal executionPrice = marketPrice.subtract(slippageAmount);
            BigDecimal totalValue     = quantity.multiply(executionPrice);
            BigDecimal fees           = totalValue.multiply(TRADING_FEE_PERCENT)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal netProceeds    = totalValue.subtract(fees);

            Optional<BotPosition> positionOpt = positionRepository
                    .findByBotIdAndSymbolAndStatus(bot.getId(), symbol, PositionStatus.OPEN);

            if (positionOpt.isEmpty()) {
                log.error("❌ No open position for symbol: {}", symbol);
                throw new RuntimeException("No open position to sell");
            }

            BotPosition position = positionOpt.get();

            if (position.getQuantity().compareTo(quantity) < 0) {
                log.error("❌ Insufficient position! Trying: {}, Available: {}", quantity, position.getQuantity());
                throw new RuntimeException("Insufficient position to sell");
            }

            BigDecimal costBasis        = position.getEntryPrice().multiply(quantity);
            BigDecimal profitLoss       = totalValue.subtract(costBasis).subtract(fees);
            BigDecimal profitLossPct    = profitLoss.divide(costBasis, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            BotTrade trade = new BotTrade();
            trade.setBotId(bot.getId());
            trade.setTradeType(TradeType.SELL);
            trade.setSymbol(symbol);
            trade.setAmount(quantity);
            trade.setPrice(executionPrice);
            trade.setTotalValue(totalValue);
            trade.setFees(fees);
            trade.setSlippage(slippageAmount);
            trade.setProfitLoss(profitLoss);
            trade.setProfitLossPercentage(profitLossPct);
            trade.setStatus(TradeStatus.FILLED);
            trade.setIsSimulation(true);
            trade.setPositionId(position.getId());
            trade.setExecutedAt(Instant.now());

            // ✅ Store indicator snapshot
            if (indicators != null && !indicators.isEmpty()) {
                trade.setIndicatorValues(buildIndicatorSnapshot(indicators));
            }

            BotTrade savedTrade = tradeRepository.save(trade);

            if (position.getQuantity().compareTo(quantity) == 0) {
                position.closePosition(executionPrice, savedTrade.getId());
                log.info("📍 Position CLOSED | entry: ${} | exit: ${} | P&L: ${} ({}%)",
                        position.getEntryPrice(), executionPrice, profitLoss, profitLossPct);
            } else {
                BigDecimal remaining      = position.getQuantity().subtract(quantity);
                BigDecimal soldEntryValue = position.getEntryPrice().multiply(quantity);
                position.setQuantity(remaining);
                position.setEntryValue(position.getEntryValue().subtract(soldEntryValue));
                position.setUpdatedAt(Instant.now());
                log.info("📍 Position REDUCED | sold: {} | remaining: {}", quantity, remaining);
            }

            positionRepository.save(position);

            bot.setCurrentBalance(bot.getCurrentBalance().add(netProceeds));
            bot.setLastExecutionTime(Instant.now());
            bot.setExecutionCount(bot.getExecutionCount() + 1);
            botRepository.save(bot);

            log.info("✅ SELL done | price: ${} | total: ${} | P&L: ${} ({}%) | balance: ${}",
                    executionPrice, totalValue, profitLoss, profitLossPct, bot.getCurrentBalance());

            return savedTrade;

        } catch (Exception e) {
            log.error("❌ SELL failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute sell order: " + e.getMessage(), e);
        }
    }

    // ─── Unrealized P&L ───────────────────────────────────────────────────────

    @Transactional
    public void updateUnrealizedPnL(TradingBot bot) {
        List<BotPosition> openPositions = positionRepository
                .findByBotIdAndStatus(bot.getId(), PositionStatus.OPEN);
        if (openPositions.isEmpty()) return;

        log.debug("📊 Updating unrealized P&L for {} positions", openPositions.size());

        for (BotPosition position : openPositions) {
            try {
                BigDecimal currentPrice = marketDataService.getCurrentPrice(position.getSymbol());
                position.updateUnrealizedPnl(currentPrice);
                positionRepository.save(position);
            } catch (Exception e) {
                log.error("❌ P&L update failed for position {}: {}", position.getId(), e.getMessage());
            }
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    /**
     * Build a clean indicator snapshot map for storage.
     * Converts BigDecimal values to Double for JSON serialization.
     * Only includes the most useful indicators — keeps the JSONB lean.
     */
    private Map<String, Object> buildIndicatorSnapshot(Map<String, BigDecimal> indicators) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        String[] keys = {
                "CLOSE_PRICE", "RSI_14", "RSI_7",
                "MACD", "MACD_SIGNAL", "MACD_HISTOGRAM",
                "MA_20", "MA_50", "MA_200",
                "EMA_12", "EMA_26",
                "BB_UPPER", "BB_MIDDLE", "BB_LOWER",
                "VOLUME"
        };

        for (String key : keys) {
            BigDecimal val = indicators.get(key);
            if (val != null) {
                snapshot.put(key, val.setScale(4, RoundingMode.HALF_UP).doubleValue());
            }
        }

        return snapshot;
    }
}