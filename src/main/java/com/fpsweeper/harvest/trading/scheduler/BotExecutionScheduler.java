package com.fpsweeper.harvest.trading.scheduler;

import com.fpsweeper.harvest.trading.*;
import com.fpsweeper.harvest.trading.service.TradeExecutionService;
import com.fpsweeper.harvest.trading.strategy.StrategyFactory;
import com.fpsweeper.harvest.trading.strategy.TradeSignal;
import com.fpsweeper.harvest.trading.strategy.TradingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class BotExecutionScheduler {

    private static final Logger log = LoggerFactory.getLogger(BotExecutionScheduler.class);

    @Autowired
    private TradingBotRepository botRepository;

    @Autowired
    private StrategyFactory strategyFactory;

    @Autowired
    private TradeExecutionService tradeExecutionService;

    /**
     * Execute all active bots every 5 minutes
     * Cron: "0 *\/5 * * * *" = At second 0, every 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void executeBots() {




        try {
            // Find all bots ready for execution
            List<TradingBot> readyBots = botRepository.findBotsReadyForExecution(Instant.now());

            if (readyBots.isEmpty()) {
                log.debug("No bots ready for execution");
                return;
            }

            log.debug("Found {} bots ready for execution", readyBots.size());

            int successCount = 0;
            int failCount = 0;

            for (TradingBot bot : readyBots) {
                try {
                    executeSingleBot(bot);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("❌ Failed to execute bot {}: {}", bot.getName(), e.getMessage());
                }
            }

            log.debug("Execution complete | Success: {} | Failed: {}", successCount, failCount);

        } catch (Exception e) {
            log.error("❌ Error in bot execution scheduler: {}", e.getMessage(), e);
        }




    }

    /**
     * Execute a single bot
     */
    /**
     * Called by BotStartedEventListener after commit to trigger immediate first execution.
     */
    public void executeSingleBot(java.util.UUID botId) {
        botRepository.findById(botId).ifPresentOrElse(
                this::executeSingleBot,
                () -> log.warn("⚠️ Bot {} not found for immediate execution", botId)
        );
    }

    public void executeSingleBot(TradingBot bot) {
        log.debug("Executing bot: {} ({})", bot.getName(), bot.getStrategyType());

        try {
            // Update unrealized P&L first
            tradeExecutionService.updateUnrealizedPnL(bot);

            // Get strategy for this bot
            TradingStrategy strategy = strategyFactory.getStrategy(bot);

            // Evaluate strategy and get trade signal
            TradeSignal signal = strategy.evaluate(bot);

            log.debug("Signal: {}", signal);

            // Execute trade if signal indicates action
            if (signal.shouldTrade()) {
                if (signal.isBuy()) {
                    tradeExecutionService.executeBuy(
                            bot,
                            signal.getSymbol(),
                            signal.getAmount(),
                            signal.getReason()
                    );
                } else if (signal.isSell()) {
                    tradeExecutionService.executeSell(
                            bot,
                            signal.getSymbol(),
                            signal.getAmount(),
                            signal.getReason()
                    );
                }
            } else {
                log.debug("Holding: {}", signal.getReason());
            }

            // Update next execution time
            updateNextExecutionTime(bot);

        } catch (Exception e) {
            log.error("❌ Error executing bot {}: {}", bot.getName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update bot's next execution time based on timeframe
     */
    private void updateNextExecutionTime(TradingBot bot) {
        // Parse timeframe and calculate next execution
        String timeframe = bot.getTimeframe();
        long minutesToAdd = parseTimeframeToMinutes(timeframe);

        Instant nextExecution = Instant.now().plusSeconds(minutesToAdd * 60);

        bot.setNextExecutionTime(nextExecution);
        botRepository.save(bot);


    }

    /**
     * Parse timeframe string to minutes
     */
    private long parseTimeframeToMinutes(String timeframe) {
        timeframe = timeframe.toLowerCase();

        try {
            if (timeframe.endsWith("m")) {
                return Long.parseLong(timeframe.replace("m", ""));
            } else if (timeframe.endsWith("h")) {
                return Long.parseLong(timeframe.replace("h", "")) * 60;
            } else if (timeframe.endsWith("d")) {
                return Long.parseLong(timeframe.replace("d", "")) * 1440;
            } else {
                return 60; // Default to 1 hour
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not parse timeframe: {}, defaulting to 60 minutes", timeframe);
            return 60;
        }
    }

    /**
     * Manual execution trigger (for testing)
     */
    public void executeBotsManually() {
        log.debug("Manual bot execution triggered");
        executeBots();
    }
}