package com.fpsweeper.harvest.trading.scheduler;

import com.fpsweeper.harvest.trading.*;
import com.fpsweeper.harvest.trading.service.BotPointsDeductionService;
import com.fpsweeper.harvest.trading.service.TradeExecutionService;
import com.fpsweeper.harvest.trading.strategy.StrategyFactory;
import com.fpsweeper.harvest.trading.strategy.TradeSignal;
import com.fpsweeper.harvest.trading.strategy.TradingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BotExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(BotExecutionScheduler.class);

    @Autowired
    private TradingBotRepository botRepository;

    @Autowired
    private StrategyFactory strategyFactory;

    @Autowired
    private TradeExecutionService tradeExecutionService;

    @Autowired
    private BotPointsDeductionService botPointsDeductionService;

    /**
     * Execute all active bots every 5 minutes.
     * Bots are only picked up when their nextExecutionTime has passed,
     * so a 4h-timeframe bot won't execute more than once every 4 hours.
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void executeBots() {
        log.info("🤖 ======================================== BOT EXECUTION CYCLE STARTED");

        try {
            List<TradingBot> readyBots = botRepository.findBotsReadyForExecution(Instant.now());

            if (readyBots.isEmpty()) {
                log.info("💤 No bots ready for execution");
                return;
            }

            log.info("🚀 Found {} bots ready for execution", readyBots.size());

            int successCount = 0;
            int failCount = 0;

            for (TradingBot bot : readyBots) {
                try {
                    executeSingleBot(bot.getId());
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ Failed to execute bot {}: {}", bot.getName(), e.getMessage());
                }
            }

            log.info("✅ Execution complete | Success: {} | Failed: {}", successCount, failCount);

        } catch (Exception e) {
            log.error("❌ Error in bot execution scheduler: {}", e.getMessage(), e);
        }

        log.info("🤖 ======================================== BOT EXECUTION CYCLE FINISHED");
    }

    /**
     * Execute a single bot inside its own transaction.
     *
     * FIX 1: We re-fetch the bot by ID here so JPA manages the entity within
     *         THIS transaction — changes to nextExecutionTime will actually be
     *         persisted on commit.
     *
     * FIX 2: nextExecutionTime is ALWAYS updated in a finally block, so even
     *         if the strategy throws an error the bot is still rescheduled and
     *         won't be retried every 5 minutes indefinitely.
     */
    @Transactional
    public void executeSingleBot(UUID botId) {
        // Re-fetch within this transaction so saves are tracked by JPA
        TradingBot bot = botRepository.findById(botId)
                .orElseThrow(() -> new RuntimeException("Bot not found: " + botId));

        log.info("⚙️ Executing bot: {} ({})", bot.getName(), bot.getStrategyType());

        try {
            // Update unrealized P&L first
            tradeExecutionService.updateUnrealizedPnL(bot);

            // Get and evaluate strategy
            TradingStrategy strategy = strategyFactory.getStrategy(bot);
            TradeSignal signal = strategy.evaluate(bot);

            log.info("📡 Signal: {}", signal);

            if (signal.shouldTrade()) {
                // Deduct points ONLY when a trade actually fires
                boolean hasPoints = botPointsDeductionService.deductPointsForTrade(
                        bot, signal.getAction().name()
                );

                if (!hasPoints) {
                    // Bot was auto-paused inside deductPointsForTrade — stop here
                    log.warn("⛔ Bot {} auto-paused — insufficient points. Trade cancelled.", bot.getName());
                    return;
                }

                if (signal.isBuy()) {
                    tradeExecutionService.executeBuy(bot, signal.getSymbol(), signal.getAmount(),
                            signal.getReason());
                } else if (signal.isSell()) {
                    tradeExecutionService.executeSell(bot, signal.getSymbol(), signal.getAmount(),
                            signal.getReason());
                }
            } else {
                log.info("⏸️ Bot holding - {}", signal.getReason());
            }

        } catch (Exception e) {
            log.error("❌ Strategy error for bot {}: {}", bot.getName(), e.getMessage(), e);
            // Don't rethrow — fall through to finally so the bot always reschedules
        } finally {
            // FIX 2: Always reschedule regardless of success or failure
            scheduleNextExecution(bot);
        }
    }

    /**
     * Compute and persist the next execution time based on the bot's timeframe.
     *
     * FIX 3: We call botRepository.save(bot) here while still inside the
     *         @Transactional method, with the entity managed by the current
     *         session — so Hibernate will actually flush the update to Postgres.
     */
    private void scheduleNextExecution(TradingBot bot) {
        long minutesToAdd = parseTimeframeToMinutes(bot.getTimeframe());
        Instant nextExecution = Instant.now().plusSeconds(minutesToAdd * 60L);

        bot.setNextExecutionTime(nextExecution);
        bot.setLastExecutionTime(Instant.now());
        botRepository.save(bot);

        log.info("⏰ [{}] Next execution in {} min → {}", bot.getName(), minutesToAdd, nextExecution);
    }

    /**
     * Parse timeframe string to minutes.
     * Supports: 1m, 5m, 15m, 30m, 1h, 4h, 1d, 1w
     */
    private long parseTimeframeToMinutes(String timeframe) {
        if (timeframe == null) return 60L;
        timeframe = timeframe.toLowerCase().trim();

        try {
            if (timeframe.endsWith("m")) {
                return Long.parseLong(timeframe.replace("m", ""));
            } else if (timeframe.endsWith("h")) {
                return Long.parseLong(timeframe.replace("h", "")) * 60L;
            } else if (timeframe.endsWith("d")) {
                return Long.parseLong(timeframe.replace("d", "")) * 1440L;
            } else if (timeframe.endsWith("w")) {
                return Long.parseLong(timeframe.replace("w", "")) * 10080L;
            }
        } catch (NumberFormatException e) {
            log.warn("⚠️ Could not parse timeframe '{}', defaulting to 60 min", timeframe);
        }

        return 60L;
    }

    /**
     * Manual execution trigger (for testing via BotExecutionController)
     */
    public void executeBotsManually() {
        log.info("🔧 Manual bot execution triggered");
        executeBots();
    }
}