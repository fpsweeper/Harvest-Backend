package com.fpsweeper.harvest.trading.service;

import com.fpsweeper.harvest.auth.exceptions.InsufficientPointsException;
import com.fpsweeper.harvest.notification.NotificationService;
import com.fpsweeper.harvest.points.PointsService;
import com.fpsweeper.harvest.trading.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class BotPointsDeductionService {

    private static final Logger log = LoggerFactory.getLogger(BotPointsDeductionService.class);

    @Autowired private PointsService pointsService;
    @Autowired private BotPointsUsageRepository botPointsUsageRepository;
    @Autowired private TradingBotRepository botRepository;
    @Autowired private NotificationService notificationService;

    /**
     * Deduct points for a trade execution.
     *
     * Called from BotExecutionScheduler ONLY when a BUY or SELL signal fires.
     * Each trade costs pointsPerDay points (configurable per bot at creation).
     *
     * Flow:
     *  1. Check user has enough points — if not, auto-pause bot and return false
     *  2. Deduct from user_points + write to point_transactions
     *  3. Accumulate into today's bot_points_usage record (create if first trade today)
     *  4. Update total_points_consumed on the bot
     *
     * Returns true if deduction succeeded, false if bot was auto-paused.
     */
    @Transactional
    public boolean deductPointsForTrade(TradingBot bot, String tradeType) {
        BigDecimal pointsToDeduct = bot.getPointsPerDay();

        // ── Pre-check: insufficient points → auto-pause ────────────────────────
        if (!pointsService.hasEnoughPoints(bot.getUserId(), pointsToDeduct)) {
            log.warn("⚠️ Insufficient points for bot: {} (user: {}). Auto-pausing.",
                    bot.getName(), bot.getUserId());

            bot.setStatus(BotStatus.PAUSED);
            bot.setPausedAt(Instant.now());
            botRepository.save(bot);

            log.info("⏸️ Bot auto-paused due to insufficient points: {}", bot.getName());

            // ✅ Fixed: was missing notification in this path
            notificationService.notifyBotAutoPaused(bot.getUserId(), bot.getName());

            return false;
        }

        try {
            String description = String.format("Bot '%s' trade executed: %s %s",
                    bot.getName(), tradeType, bot.getTradingPair());

            pointsService.deductPoints(
                    bot.getUserId(),
                    pointsToDeduct,
                    description,
                    bot.getId()
            );

            accumulateDailyUsage(bot, pointsToDeduct);

            BigDecimal newTotal = bot.getTotalPointsConsumed().add(pointsToDeduct);
            bot.setTotalPointsConsumed(newTotal);

            log.info("💰 Deducted {} points for {} trade on bot: {} (total consumed: {})",
                    pointsToDeduct, tradeType, bot.getName(), newTotal);

            return true;

        } catch (InsufficientPointsException e) {
            // Race condition safety net — another thread deducted points between the check above
            log.warn("⚠️ Race condition — insufficient points for bot: {}", bot.getName());

            bot.setStatus(BotStatus.PAUSED);
            bot.setPausedAt(Instant.now());
            botRepository.save(bot);

            notificationService.notifyBotAutoPaused(bot.getUserId(), bot.getName());

            return false;
        }
    }

    /**
     * Upsert today's bot_points_usage record.
     * First trade of the day → create new record.
     * Subsequent trades → accumulate on existing record.
     */
    private void accumulateDailyUsage(TradingBot bot, BigDecimal pointsToDeduct) {
        LocalDate today = LocalDate.now();

        Optional<BotPointsUsage> existing = botPointsUsageRepository
                .findByBotIdAndUsageDate(bot.getId(), today);

        if (existing.isPresent()) {
            BotPointsUsage usage = existing.get();
            usage.setPointsConsumed(usage.getPointsConsumed().add(pointsToDeduct));
            botPointsUsageRepository.save(usage);
        } else {
            BotPointsUsage usage = new BotPointsUsage(
                    bot.getId(),
                    bot.getUserId(),
                    today,
                    pointsToDeduct,
                    bot.getPointsPerDay(),
                    bot.getStatus().name()
            );
            botPointsUsageRepository.save(usage);
        }
    }

    /**
     * Check if user has enough points without deducting or pausing.
     */
    public boolean hasEnoughPoints(TradingBot bot) {
        return pointsService.hasEnoughPoints(bot.getUserId(), bot.getPointsPerDay());
    }
}