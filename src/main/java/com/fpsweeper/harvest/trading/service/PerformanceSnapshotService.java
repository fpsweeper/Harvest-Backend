package com.fpsweeper.harvest.trading.service;

import com.fpsweeper.harvest.trading.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
public class PerformanceSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(PerformanceSnapshotService.class);

    @Autowired private TradingBotRepository      botRepository;
    @Autowired private BotPerformanceSnapshotRepository snapshotRepository;
    @Autowired private BotTradeRepository         tradeRepository;
    @Autowired private BotPositionRepository      positionRepository;
    @Autowired private TradeExecutionService      tradeExecutionService;

    // ── Unrealized P&L refresh ─────────────────────────────────────────────
    // Runs every 5 minutes for ALL running bots, regardless of their timeframe.
    // This is what makes the P&L values in the UI feel live.

    @Scheduled(cron = "0 */5 * * * *")
    public void refreshUnrealizedPnL() {
        List<TradingBot> activeBots = botRepository.findByStatus(BotStatus.SIMULATING);

        if (activeBots.isEmpty()) return;

        log.info("💹 Refreshing unrealized P&L for {} running bots", activeBots.size());

        for (TradingBot bot : activeBots) {
            try {
                tradeExecutionService.updateUnrealizedPnL(bot);
            } catch (Exception e) {
                log.error("❌ Failed to update P&L for bot {}: {}", bot.getName(), e.getMessage());
            }
        }
    }

    // ── Performance snapshots ──────────────────────────────────────────────
    // Runs every 5 minutes (shifted 2.5 min from P&L refresh so they don't
    // compete). Captures a balance/P&L snapshot for the equity curve chart.

    @Scheduled(cron = "30 2/5 * * * *")   // at :30 seconds past every 5th minute
    public void createSnapshots() {
        List<TradingBot> activeBots = botRepository.findAllActiveOrPaused();

        if (activeBots.isEmpty()) return;

        log.info("📸 Creating performance snapshots for {} bots", activeBots.size());

        for (TradingBot bot : activeBots) {
            try {
                createSnapshot(bot, SnapshotType.HOURLY); // reuse HOURLY type — no schema change needed
            } catch (Exception e) {
                log.error("❌ Error creating snapshot for bot {}: {}", bot.getName(), e.getMessage());
            }
        }
    }

    // ── Snapshot builder ───────────────────────────────────────────────────

    @Transactional
    public BotPerformanceSnapshot createSnapshot(TradingBot bot, SnapshotType type) {
        log.debug("📊 Creating {} snapshot for bot: {}", type, bot.getName());

        BotPerformanceSnapshot snapshot = new BotPerformanceSnapshot();
        snapshot.setBotId(bot.getId());
        snapshot.setSnapshotType(type);
        snapshot.setBalance(bot.getCurrentBalance());
        snapshot.setInitialBalance(bot.getInitialBalance());

        // P&L — read from positions (already updated by refreshUnrealizedPnL)
        BigDecimal realizedPnl   = nullSafe(positionRepository.getTotalRealizedPnl(bot.getId()));
        BigDecimal unrealizedPnl = nullSafe(positionRepository.getTotalUnrealizedPnl(bot.getId()));
        BigDecimal totalPnl      = realizedPnl.add(unrealizedPnl);

        snapshot.setRealizedPnl(realizedPnl);
        snapshot.setUnrealizedPnl(unrealizedPnl);
        snapshot.setTotalPnl(totalPnl);

        if (bot.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            snapshot.setTotalPnlPercentage(
                    totalPnl.divide(bot.getInitialBalance(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
            );
        }

        // Trade stats
        long totalTrades    = tradeRepository.countByBotId(bot.getId());
        long winningTrades  = tradeRepository.countProfitableTradesByBotId(bot.getId());
        long losingTrades   = tradeRepository.countLosingTradesByBotId(bot.getId());

        snapshot.setTotalTrades((int) totalTrades);
        snapshot.setWinningTrades((int) winningTrades);
        snapshot.setLosingTrades((int) losingTrades);
        snapshot.calculateWinRate();

        // Position stats
        long openCount          = positionRepository.countByBotIdAndStatus(bot.getId(), PositionStatus.OPEN);
        BigDecimal openValue    = nullSafe(positionRepository.getTotalOpenPositionsValue(bot.getId()));

        snapshot.setOpenPositionsCount((int) openCount);
        snapshot.setOpenPositionsValue(openValue);

        // Trade performance
        snapshot.setAverageWin(tradeRepository.getAverageWin(bot.getId()));
        snapshot.setAverageLoss(tradeRepository.getAverageLoss(bot.getId()));
        snapshot.setLargestWin(tradeRepository.getLargestWin(bot.getId()));
        snapshot.setLargestLoss(tradeRepository.getLargestLoss(bot.getId()));

        BigDecimal avgWin  = snapshot.getAverageWin();
        BigDecimal avgLoss = snapshot.getAverageLoss();
        if (avgWin != null && avgLoss != null && avgLoss.compareTo(BigDecimal.ZERO) != 0) {
            snapshot.setProfitFactor(avgWin.abs().divide(avgLoss.abs(), 4, RoundingMode.HALF_UP));
        }

        BotPerformanceSnapshot saved = snapshotRepository.save(snapshot);

        log.debug("✅ Snapshot saved | P&L: ${} ({}%) | Win Rate: {}% | Trades: {}",
                totalPnl, snapshot.getTotalPnlPercentage(), snapshot.getWinRate(), totalTrades);

        return saved;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}