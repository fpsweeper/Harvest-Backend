package com.fpsweeper.harvest.trading.service;

import com.fpsweeper.harvest.trading.*;
import com.fpsweeper.harvest.trading.dto.BotResponse;
import com.fpsweeper.harvest.trading.dto.CreateBotRequest;
import com.fpsweeper.harvest.trading.dto.IndicatorConditionRequest;
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
    @Autowired private com.fpsweeper.harvest.notification.NotificationService notificationService;
    @Autowired private com.fpsweeper.harvest.user.UserRepository userRepository;

    @Transactional
    public BotResponse createBot(CreateBotRequest request, UUID userId) {
        log.info("🤖 Creating new bot: {} for user: {}", request.getName(), userId);

        // Create bot entity
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
        bot.setPointsPerDay(request.getPointsPerDay());
        bot.setConfiguration(request.getConfiguration() != null ? request.getConfiguration() : new HashMap<>());
        bot.setExecutionCount(0);

        TradingBot savedBot = botRepository.save(bot);

        // Create entry conditions
        if (request.getEntryConditions() != null && !request.getEntryConditions().isEmpty()) {
            saveConditions(savedBot.getId(), request.getEntryConditions(), ConditionType.ENTRY);
        }

        // Create exit conditions
        if (request.getExitConditions() != null && !request.getExitConditions().isEmpty()) {
            saveConditions(savedBot.getId(), request.getExitConditions(), ConditionType.EXIT);
        }

        log.info("✅ Bot created successfully: {} (ID: {})", savedBot.getName(), savedBot.getId());

        return convertToResponse(savedBot);
    }

    /**
     * Save indicator conditions
     */
    private void saveConditions(UUID botId, List<IndicatorConditionRequest> conditions, ConditionType type) {
        int order = 0;
        for (IndicatorConditionRequest condReq : conditions) {
            BotIndicatorCondition condition = new BotIndicatorCondition();
            condition.setBotId(botId);
            condition.setConditionType(type);
            condition.setIndicatorName(condReq.getIndicatorName());
            condition.setIndicatorPeriod(condReq.getIndicatorPeriod());
            condition.setOperator(condReq.getOperator());
            condition.setComparisonValue(condReq.getComparisonValue());
            condition.setLogicalOperator(condReq.getLogicalOperator());
            condition.setConditionOrder(order++);

            conditionRepository.save(condition);
        }


    }

    /**
     * Get all bots for a user
     */
    public List<BotResponse> getUserBots(UUID userId) {
        List<TradingBot> bots = botRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return bots.stream()
                .filter(b -> b.getStatus() != BotStatus.DELETED)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get bot by ID
     */
    public BotResponse getBotById(UUID botId, UUID userId) {
        TradingBot bot = botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));

        return convertToResponse(bot);
    }

    /**
     * Start a bot
     */
    @Transactional
    public BotResponse startBot(UUID botId, UUID userId) {
        TradingBot bot = botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));

        if (!bot.canStart()) {
            throw new RuntimeException("Bot cannot be started in current status: " + bot.getStatus());
        }

        bot.setStatus(BotStatus.SIMULATING);
        bot.setStartedAt(Instant.now());
        bot.setNextExecutionTime(Instant.now()); // Execute immediately on next scheduler run

        TradingBot savedBot = botRepository.save(bot);

        log.info("▶️ Bot started: {} (ID: {})", savedBot.getName(), savedBot.getId());

        return convertToResponse(savedBot);
    }

    /**
     * Pause a bot
     */
    @Transactional
    public BotResponse pauseBot(UUID botId, UUID userId) {
        TradingBot bot = botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));

        if (!bot.canPause()) {
            throw new RuntimeException("Bot cannot be paused in current status: " + bot.getStatus());
        }

        bot.setStatus(BotStatus.PAUSED);
        bot.setPausedAt(Instant.now());

        TradingBot savedBot = botRepository.save(bot);

        log.info("⏸️ Bot paused: {} (ID: {})", savedBot.getName(), savedBot.getId());

        return convertToResponse(savedBot);
    }

    /**
     * Stop a bot (closes all positions)
     */
    @Transactional
    public BotResponse stopBot(UUID botId, UUID userId) {
        TradingBot bot = botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));

        if (!bot.canStop()) {
            throw new RuntimeException("Bot cannot be stopped in current status: " + bot.getStatus());
        }

        // TODO: Close all open positions before stopping
        // For now, just change status

        bot.setStatus(BotStatus.STOPPED);
        bot.setStoppedAt(Instant.now());

        TradingBot savedBot = botRepository.save(bot);

        log.info("⏹️ Bot stopped: {} (ID: {})", savedBot.getName(), savedBot.getId());

        return convertToResponse(savedBot);
    }

    /**
     * Delete a bot
     */
    @Transactional
    public void deleteBot(UUID botId, UUID userId) {
        TradingBot bot = botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));

        if (bot.getStatus() == BotStatus.DELETED) {
            throw new RuntimeException("Bot is already deleted");
        }

        // Auto-stop if running or paused — no need to force user to stop first
        if (bot.getStatus() == BotStatus.SIMULATING || bot.getStatus() == BotStatus.PAUSED) {
            bot.setStatus(BotStatus.STOPPED);
            bot.setStoppedAt(Instant.now());
        }

        bot.setStatus(BotStatus.DELETED);
        bot.setDeletedAt(Instant.now());
        botRepository.save(bot);

        notificationService.notifyBotDeleted(userId, bot.getName());
        log.info("🗑️ Bot deleted: {} (ID: {})", bot.getName(), bot.getId());
    }

    /**
     * Convert entity to response DTO
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

        // Calculate P&L
        BigDecimal totalPnl = bot.getCurrentBalance().subtract(bot.getInitialBalance());
        response.setTotalPnl(totalPnl);

        if (bot.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pnlPercent = totalPnl
                    .divide(bot.getInitialBalance(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            response.setTotalPnlPercentage(pnlPercent);
        }

        // Count trades and positions
        response.setTotalTrades((int) tradeRepository.countByBotId(bot.getId()));
        response.setOpenPositions((int) positionRepository.countByBotIdAndStatus(bot.getId(), PositionStatus.OPEN));

        return response;
    }
}