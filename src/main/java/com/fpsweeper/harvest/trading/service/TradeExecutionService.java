package com.fpsweeper.harvest.trading.service;

import com.fpsweeper.harvest.notification.NotificationService;
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

    @Autowired private BotTradeRepository tradeRepository;
    @Autowired private BotPositionRepository positionRepository;
    @Autowired private TradingBotRepository botRepository;
    @Autowired private MarketDataService marketDataService;
    @Autowired private NotificationService notificationService;

    @Transactional
    public BotTrade executeBuy(TradingBot bot, String symbol, BigDecimal quantity, String reason) {
        log.info("🛒 Executing BUY order for bot: {} | Symbol: {} | Quantity: {}",
                bot.getName(), symbol, quantity);

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

            BotTrade savedTrade = tradeRepository.save(trade);

            Optional<BotPosition> existingPosition = positionRepository
                    .findByBotIdAndSymbolAndStatus(bot.getId(), symbol, PositionStatus.OPEN);

            BotPosition position;
            if (existingPosition.isPresent()) {
                position = existingPosition.get();
                BigDecimal oldQuantity = position.getQuantity();
                BigDecimal oldValue    = position.getEntryValue();
                BigDecimal newQuantity = oldQuantity.add(quantity);
                BigDecimal newValue    = oldValue.add(totalValue);
                BigDecimal newAvgPrice = newValue.divide(newQuantity, 2, RoundingMode.HALF_UP);
                position.setQuantity(newQuantity);
                position.setEntryValue(newValue);
                position.setEntryPrice(newAvgPrice);
                position.setUpdatedAt(Instant.now());
                log.info("📊 Updated position | Avg Price: ${} → ${}", oldValue.divide(oldQuantity, 2, RoundingMode.HALF_UP), newAvgPrice);
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
                log.info("📍 Created new position | Entry: ${}", executionPrice);
            }

            positionRepository.save(position);
            bot.setCurrentBalance(bot.getCurrentBalance().subtract(totalCost));
            bot.setLastExecutionTime(Instant.now());
            bot.setExecutionCount(bot.getExecutionCount() + 1);
            botRepository.save(bot);

            log.info("✅ BUY executed successfully | Price: ${} | Total: ${} | Fees: ${} | New Balance: ${}",
                    executionPrice, totalValue, fees, bot.getCurrentBalance());

            // 🔔 Notify user of buy
            notificationService.notifyBotBuy(
                    bot.getUserId(), bot.getName(), symbol, quantity, executionPrice);

            return savedTrade;

        } catch (Exception e) {
            log.error("❌ Error executing BUY order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute buy order: " + e.getMessage(), e);
        }
    }

    @Transactional
    public BotTrade executeSell(TradingBot bot, String symbol, BigDecimal quantity, String reason) {
        log.info("💰 Executing SELL order for bot: {} | Symbol: {} | Quantity: {}",
                bot.getName(), symbol, quantity);

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

            if (!positionOpt.isPresent()) {
                log.error("❌ No open position found for symbol: {}", symbol);
                throw new RuntimeException("No open position to sell");
            }

            BotPosition position = positionOpt.get();

            if (position.getQuantity().compareTo(quantity) < 0) {
                log.error("❌ Insufficient position! Trying to sell: {}, Available: {}",
                        quantity, position.getQuantity());
                throw new RuntimeException("Insufficient position to sell");
            }

            BigDecimal costBasis         = position.getEntryPrice().multiply(quantity);
            BigDecimal profitLoss        = totalValue.subtract(costBasis).subtract(fees);
            BigDecimal profitLossPercent = profitLoss.divide(costBasis, 4, RoundingMode.HALF_UP)
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
            trade.setProfitLossPercentage(profitLossPercent);
            trade.setStatus(TradeStatus.FILLED);
            trade.setIsSimulation(true);
            trade.setPositionId(position.getId());
            trade.setExecutedAt(Instant.now());

            BotTrade savedTrade = tradeRepository.save(trade);

            if (position.getQuantity().compareTo(quantity) == 0) {
                position.closePosition(executionPrice, savedTrade.getId());
                log.info("📍 Position CLOSED | Entry: ${} | Exit: ${} | P&L: ${} ({}%)",
                        position.getEntryPrice(), executionPrice, profitLoss, profitLossPercent);
            } else {
                BigDecimal remainingQuantity = position.getQuantity().subtract(quantity);
                BigDecimal soldValue         = position.getEntryPrice().multiply(quantity);
                BigDecimal remainingValue    = position.getEntryValue().subtract(soldValue);
                position.setQuantity(remainingQuantity);
                position.setEntryValue(remainingValue);
                position.setUpdatedAt(Instant.now());
                log.info("📍 Position REDUCED | Sold: {} | Remaining: {}", quantity, remainingQuantity);
            }

            positionRepository.save(position);
            bot.setCurrentBalance(bot.getCurrentBalance().add(netProceeds));
            bot.setLastExecutionTime(Instant.now());
            bot.setExecutionCount(bot.getExecutionCount() + 1);
            botRepository.save(bot);

            log.info("✅ SELL executed successfully | Price: ${} | Total: ${} | Fees: ${} | P&L: ${} ({}%) | New Balance: ${}",
                    executionPrice, totalValue, fees, profitLoss, profitLossPercent, bot.getCurrentBalance());

            // 🔔 Notify user — detect TP/SL from reason string to send richer notification
            String reasonLower = reason != null ? reason.toLowerCase() : "";
            if (reasonLower.contains("take profit")) {
                notificationService.notifyTakeProfit(
                        bot.getUserId(), bot.getName(), symbol, profitLoss, profitLossPercent);
            } else if (reasonLower.contains("stop loss")) {
                notificationService.notifyStopLoss(
                        bot.getUserId(), bot.getName(), symbol, profitLoss, profitLossPercent);
            } else {
                notificationService.notifyBotSell(
                        bot.getUserId(), bot.getName(), symbol, quantity, executionPrice, profitLoss);
            }

            return savedTrade;

        } catch (Exception e) {
            log.error("❌ Error executing SELL order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute sell order: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void updateUnrealizedPnL(TradingBot bot) {
        List<BotPosition> openPositions = positionRepository.findByBotIdAndStatus(
                bot.getId(), PositionStatus.OPEN);

        if (openPositions.isEmpty()) return;

        log.debug("📊 Updating unrealized P&L for {} open positions", openPositions.size());

        for (BotPosition position : openPositions) {
            try {
                BigDecimal currentPrice = marketDataService.getCurrentPrice(position.getSymbol());
                position.updateUnrealizedPnl(currentPrice);
                positionRepository.save(position);
                log.debug("   {} - Entry: ${}, Current: ${}, Unrealized P&L: ${} ({}%)",
                        position.getSymbol(), position.getEntryPrice(), currentPrice,
                        position.getUnrealizedPnl(), position.getUnrealizedPnlPercentage());
            } catch (Exception e) {
                log.error("❌ Error updating P&L for position {}: {}", position.getId(), e.getMessage());
            }
        }
    }
}