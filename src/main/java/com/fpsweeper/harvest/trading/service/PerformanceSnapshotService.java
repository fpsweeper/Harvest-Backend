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

    @Autowired
    private TradingBotRepository botRepository;

    @Autowired
    private BotPerformanceSnapshotRepository snapshotRepository;

    @Autowired
    private BotTradeRepository tradeRepository;

    @Autowired
    private BotPositionRepository positionRepository;

    /**
     * Create hourly snapshots for all active and paused bots.
     * Runs at the top of every hour.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void createHourlySnapshots() {
        log.info("📸 Creating hourly performance snapshots");

        List<TradingBot> activeBots = botRepository.findAllActiveOrPaused();

        for (TradingBot bot : activeBots) {
            try {
                createSnapshot(bot, SnapshotType.HOURLY);
            } catch (Exception e) {
                log.error("❌ Error creating snapshot for bot {}: {}", bot.getName(), e.getMessage());
            }
        }

        log.info("✅ Hourly snapshots created for {} bots", activeBots.size());
    }

    /**
     * Create a performance snapshot for a bot.
     *
     * FIX: snapshotTime was relying on the field default (Instant.now() at
     * object creation), which is fine but we now set it explicitly at the
     * moment of snapshot creation so the timestamp is precise and testable.
     */
    @Transactional
    public BotPerformanceSnapshot createSnapshot(TradingBot bot, SnapshotType type) {
        log.debug("📊 Creating {} snapshot for bot: {}", type, bot.getName());

        Instant now = Instant.now(); // capture once so all fields use the same timestamp

        BotPerformanceSnapshot snapshot = new BotPerformanceSnapshot();
        snapshot.setBotId(bot.getId());
        snapshot.setSnapshotType(type);
        snapshot.setSnapshotTime(now); // FIX: explicit set
        snapshot.setBalance(bot.getCurrentBalance());
        snapshot.setInitialBalance(bot.getInitialBalance());

        // P&L
        BigDecimal realizedPnl = positionRepository.getTotalRealizedPnl(bot.getId());
        BigDecimal unrealizedPnl = positionRepository.getTotalUnrealizedPnl(bot.getId());
        BigDecimal totalPnl = realizedPnl.add(unrealizedPnl);

        snapshot.setRealizedPnl(realizedPnl);
        snapshot.setUnrealizedPnl(unrealizedPnl);
        snapshot.setTotalPnl(totalPnl);

        if (bot.getInitialBalance().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalPnlPercent = totalPnl
                    .divide(bot.getInitialBalance(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            snapshot.setTotalPnlPercentage(totalPnlPercent);
        }

        // Trade statistics
        long totalTrades = tradeRepository.countByBotId(bot.getId());
        long winningTrades = tradeRepository.countProfitableTradesByBotId(bot.getId());
        long losingTrades = tradeRepository.countLosingTradesByBotId(bot.getId());

        snapshot.setTotalTrades((int) totalTrades);
        snapshot.setWinningTrades((int) winningTrades);
        snapshot.setLosingTrades((int) losingTrades);
        snapshot.calculateWinRate();

        // Position metrics
        long openPositionsCount = positionRepository.countByBotIdAndStatus(bot.getId(), PositionStatus.OPEN);
        BigDecimal openPositionsValue = positionRepository.getTotalOpenPositionsValue(bot.getId());

        snapshot.setOpenPositionsCount((int) openPositionsCount);
        snapshot.setOpenPositionsValue(openPositionsValue);

        // Trade performance
        snapshot.setAverageWin(tradeRepository.getAverageWin(bot.getId()));
        snapshot.setAverageLoss(tradeRepository.getAverageLoss(bot.getId()));
        snapshot.setLargestWin(tradeRepository.getLargestWin(bot.getId()));
        snapshot.setLargestLoss(tradeRepository.getLargestLoss(bot.getId()));

        // Profit factor
        BigDecimal avgWin = snapshot.getAverageWin();
        BigDecimal avgLoss = snapshot.getAverageLoss();
        if (avgWin != null && avgLoss != null && avgLoss.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal profitFactor = avgWin.abs().divide(avgLoss.abs(), 4, RoundingMode.HALF_UP);
            snapshot.setProfitFactor(profitFactor);
        }

        BotPerformanceSnapshot saved = snapshotRepository.save(snapshot);

        log.debug("✅ Snapshot saved | P&L: ${} ({}%) | Win Rate: {}% | Trades: {}",
                totalPnl, snapshot.getTotalPnlPercentage(), snapshot.getWinRate(), totalTrades);

        return saved;
    }
}