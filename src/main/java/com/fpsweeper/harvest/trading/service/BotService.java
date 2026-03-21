package com.fpsweeper.harvest.trading.service;

import com.fpsweeper.harvest.trading.*;
import com.fpsweeper.harvest.trading.dto.BotResponse;
import com.fpsweeper.harvest.trading.dto.CreateBotRequest;
import com.fpsweeper.harvest.trading.dto.IndicatorConditionRequest;
import com.fpsweeper.harvest.trading.dto.UpdateBotRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BotService {

    private static final Logger log = LoggerFactory.getLogger(BotService.class);

    @Autowired private TradingBotRepository botRepository;
    @Autowired private BotIndicatorConditionRepository conditionRepository;
    @Autowired private BotTradeRepository tradeRepository;
    @Autowired private BotPositionRepository positionRepository;
    @Autowired private TradeExecutionService tradeExecutionService;

    // ─── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public BotResponse createBot(CreateBotRequest request, UUID userId) {
        log.info("🤖 Creating new bot: {} for user: {}", request.getName(), userId);

        TradingBot bot = new TradingBot();
        bot.setUserId(userId);
        bot.setName(request.getName());
        bot.setDescription(request.getDescription());
        bot.setStrategyType(request.getStrategyType());
        bot.setTradingPair(request.getTradingPair());
        bot.setTimeframe(request.getTimeframe());
        bot.setStatus(BotStatus.CREATED);
        bot.setInitialBalance(request.getInitialBalance());
        bot.setCurrentBalance(request.getInitialBalance());
        bot.setStopLossPercentage(request.getStopLossPercentage());
        bot.setTakeProfitPercentage(request.getTakeProfitPercentage());
        bot.setMaxPositionSizePercentage(request.getMaxPositionSizePercentage());
        bot.setPointsPerDay(StrategyPointsCost.forStrategy(request.getStrategyType()));
        bot.setConfiguration(request.getConfiguration() != null ? request.getConfiguration() : new HashMap<>());
        bot.setExecutionCount(0);

        TradingBot savedBot = botRepository.save(bot);

        if (request.getEntryConditions() != null && !request.getEntryConditions().isEmpty())
            saveConditions(savedBot.getId(), request.getEntryConditions(), ConditionType.ENTRY);
        if (request.getExitConditions() != null && !request.getExitConditions().isEmpty())
            saveConditions(savedBot.getId(), request.getExitConditions(), ConditionType.EXIT);

        log.info("✅ Bot created: {} (ID: {})", savedBot.getName(), savedBot.getId());
        return convertToResponse(savedBot);
    }

    // ─── Read ──────────────────────────────────────────────────────────────────

    public List<BotResponse> getUserBots(UUID userId) {
        return botRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public BotResponse getBotById(UUID botId, UUID userId) {
        TradingBot bot = botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));
        return convertToResponse(bot);
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    @Transactional
    public BotResponse startBot(UUID botId, UUID userId) {
        TradingBot bot = botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));

        if (!bot.canStart())
            throw new RuntimeException("Bot cannot be started in current status: " + bot.getStatus());

        bot.setStatus(BotStatus.SIMULATING);
        bot.setStartedAt(Instant.now());
        bot.setNextExecutionTime(Instant.now());

        TradingBot saved = botRepository.save(bot);
        log.info("▶️ Bot started: {} (ID: {})", saved.getName(), saved.getId());
        return convertToResponse(saved);
    }

    @Transactional
    public BotResponse pauseBot(UUID botId, UUID userId) {
        TradingBot bot = botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));

        if (!bot.canPause())
            throw new RuntimeException("Bot cannot be paused in current status: " + bot.getStatus());

        bot.setStatus(BotStatus.PAUSED);
        bot.setPausedAt(Instant.now());

        TradingBot saved = botRepository.save(bot);
        log.info("⏸️ Bot paused: {} (ID: {})", saved.getName(), saved.getId());
        return convertToResponse(saved);
    }

    @Transactional
    public BotResponse stopBot(UUID botId, UUID userId) {
        TradingBot bot = botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));

        if (!bot.canStop())
            throw new RuntimeException("Bot cannot be stopped in current status: " + bot.getStatus());

        // Close all open positions at market price before stopping
        List<BotPosition> openPositions = positionRepository
                .findByBotIdAndStatus(botId, PositionStatus.OPEN);

        if (!openPositions.isEmpty()) {
            log.info("🔒 Closing {} open position(s) for bot: {}", openPositions.size(), bot.getName());
            for (BotPosition position : openPositions) {
                try {
                    tradeExecutionService.executeSell(
                            bot,
                            position.getSymbol(),
                            position.getQuantity(),
                            "Position closed — bot stopped"
                    );
                } catch (Exception e) {
                    log.error("❌ Failed to close position {}: {}", position.getId(), e.getMessage());
                }
            }
        }

        bot.setStatus(BotStatus.STOPPED);
        bot.setStoppedAt(Instant.now());

        TradingBot saved = botRepository.save(bot);
        log.info("⏹️ Bot stopped: {} (ID: {})", saved.getName(), saved.getId());
        return convertToResponse(saved);
    }

    @Transactional
    public void deleteBot(UUID botId, UUID userId) {
        TradingBot bot = botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));

        if (!bot.canDelete())
            throw new RuntimeException("Bot cannot be deleted in current status: " + bot.getStatus());

        bot.setStatus(BotStatus.DELETED);
        bot.setDeletedAt(Instant.now());
        botRepository.save(bot);
        log.info("🗑️ Bot deleted: {} (ID: {})", bot.getName(), bot.getId());
    }

    // ─── Update ────────────────────────────────────────────────────────────────

    @Transactional
    public BotResponse updateBot(UUID botId, UUID userId, UpdateBotRequest request) {
        TradingBot bot = botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));

        if (bot.getStatus() == BotStatus.SIMULATING)
            throw new RuntimeException("Pause the bot before editing its configuration");

        long openCount = positionRepository.countByBotIdAndStatus(botId, PositionStatus.OPEN);
        if (openCount > 0)
            throw new RuntimeException(
                    "Cannot edit bot while it has " + openCount +
                            " open position(s). Stop the bot first to close them.");

        if (request.getName() != null)                      bot.setName(request.getName());
        if (request.getDescription() != null)               bot.setDescription(request.getDescription());
        if (request.getTradingPair() != null)               bot.setTradingPair(request.getTradingPair());
        if (request.getTimeframe() != null)                 bot.setTimeframe(request.getTimeframe());
        if (request.getStopLossPercentage() != null)        bot.setStopLossPercentage(request.getStopLossPercentage());
        if (request.getTakeProfitPercentage() != null)      bot.setTakeProfitPercentage(request.getTakeProfitPercentage());
        if (request.getMaxPositionSizePercentage() != null) bot.setMaxPositionSizePercentage(request.getMaxPositionSizePercentage());
        if (request.getConfiguration() != null)             bot.setConfiguration(request.getConfiguration());

        bot.setNextExecutionTime(null);

        TradingBot saved = botRepository.save(bot);

        if (request.getEntryConditions() != null || request.getExitConditions() != null) {
            conditionRepository.deleteByBotId(botId);
            if (request.getEntryConditions() != null)
                saveConditions(botId, request.getEntryConditions(), ConditionType.ENTRY);
            if (request.getExitConditions() != null)
                saveConditions(botId, request.getExitConditions(), ConditionType.EXIT);
        }

        log.info("✏️ Bot updated: {} (ID: {})", saved.getName(), saved.getId());
        return convertToResponse(saved);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void saveConditions(UUID botId, List<IndicatorConditionRequest> conditions, ConditionType type) {
        int order = 0;
        for (IndicatorConditionRequest req : conditions) {
            BotIndicatorCondition cond = new BotIndicatorCondition();
            cond.setBotId(botId);
            cond.setConditionType(type);
            cond.setIndicatorName(req.getIndicatorName());
            cond.setIndicatorPeriod(req.getIndicatorPeriod());
            cond.setOperator(req.getOperator());
            cond.setComparisonValue(req.getComparisonValue());
            cond.setLogicalOperator(req.getLogicalOperator());
            cond.setConditionOrder(order++);
            conditionRepository.save(cond);
        }
        log.debug("💾 Saved {} {} conditions", conditions.size(), type);
    }

    /**
     * Convert bot entity to response DTO.
     *
     * P&L FIX: Previous formula was currentBalance - initialBalance which
     * was WRONG. When a bot buys $400 of BTC, currentBalance drops by $400
     * but the position is worth $400 — so it showed -$400 P&L even though
     * nothing was lost.
     *
     * Correct formula:
     *   totalPnl = realizedPnl + unrealizedPnl
     *
     * realizedPnl   = SUM of profit_loss on all SELL trades (closed positions)
     * unrealizedPnl = SUM of (currentPrice - entryPrice) * qty on OPEN positions
     *                 updated each execution cycle by updateUnrealizedPnL()
     *
     * Examples:
     *   Buy $400 BTC, price unchanged → 0 + 0 = $0 P&L ✅
     *   BTC up 5%                     → 0 + $20 = +$20 ✅
     *   Sell at +$20 profit            → +$20 + 0 = +$20 ✅
     *   Sell at -$30 loss              → -$30 + 0 = -$30 ✅
     */
    private BotResponse convertToResponse(TradingBot bot) {
        BotResponse response = new BotResponse();
        response.setId(bot.getId());
        response.setName(bot.getName());
        response.setDescription(bot.getDescription());
        response.setStrategyType(bot.getStrategyType());
        response.setTradingPair(bot.getTradingPair());
        response.setTimeframe(bot.getTimeframe());
        response.setStatus(bot.getStatus());
        response.setInitialBalance(bot.getInitialBalance());
        response.setCurrentBalance(bot.getCurrentBalance());
        response.setStopLossPercentage(bot.getStopLossPercentage());
        response.setTakeProfitPercentage(bot.getTakeProfitPercentage());
        response.setMaxPositionSizePercentage(bot.getMaxPositionSizePercentage());
        response.setCreatedAt(bot.getCreatedAt());
        response.setStartedAt(bot.getStartedAt());
        response.setLastExecutionTime(bot.getLastExecutionTime());
        response.setNextExecutionTime(bot.getNextExecutionTime());
        response.setConfiguration(bot.getConfiguration());

        // ── Correct P&L: realized + unrealized ─────────────────────────────
        BigDecimal realizedPnl   = positionRepository.getTotalRealizedPnl(bot.getId());
        BigDecimal unrealizedPnl = positionRepository.getTotalUnrealizedPnl(bot.getId());
        BigDecimal totalPnl      = realizedPnl.add(unrealizedPnl);

        response.setTotalPnl(totalPnl);

        if (bot.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pnlPercent = totalPnl
                    .divide(bot.getInitialBalance(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            response.setTotalPnlPercentage(pnlPercent);
        }
        // ───────────────────────────────────────────────────────────────────

        response.setTotalTrades((int) tradeRepository.countByBotId(bot.getId()));
        response.setOpenPositions((int) positionRepository.countByBotIdAndStatus(
                bot.getId(), PositionStatus.OPEN));

        return response;
    }
}