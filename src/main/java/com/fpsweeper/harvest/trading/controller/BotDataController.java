package com.fpsweeper.harvest.trading.controller;

import com.fpsweeper.harvest.trading.*;
import com.fpsweeper.harvest.user.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bots/{botId}")
@CrossOrigin(origins = "*")
public class BotDataController {

    private static final Logger log = LoggerFactory.getLogger(BotDataController.class);

    @Autowired
    private BotTradeRepository tradeRepository;

    @Autowired
    private BotPositionRepository positionRepository;

    @Autowired
    private BotPerformanceSnapshotRepository snapshotRepository;

    @Autowired
    private BotIndicatorConditionRepository conditionRepository;

    @Autowired
    private TradingBotRepository botRepository;

    // ── Auth helper ───────────────────────────────────────────────────────────

    /**
     * Verifies the bot belongs to the authenticated user.
     * Throws RuntimeException (→ 400) if not found or not owned.
     */
    private TradingBot verifyBotOwnership(UUID botId, Users user) {
        if (user == null) throw new RuntimeException("Unauthorized");
        return botRepository.findByIdAndUserId(botId, user.getId())
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));
    }

    // ── Trades ────────────────────────────────────────────────────────────────

    /**
     * GET /api/bots/{botId}/trades?page=0&size=20
     */
    @GetMapping("/trades")
    public ResponseEntity<?> getBotTrades(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        try {
            verifyBotOwnership(botId, user);

            Pageable pageable = PageRequest.of(page, size);
            Page<BotTrade> trades = tradeRepository.findByBotIdOrderByExecutedAtDesc(botId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("trades", trades.getContent());
            response.put("totalTrades", trades.getTotalElements());
            response.put("totalPages", trades.getTotalPages());
            response.put("currentPage", page);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching trades for bot {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Positions ─────────────────────────────────────────────────────────────

    /**
     * GET /api/bots/{botId}/positions
     */
    @GetMapping("/positions")
    public ResponseEntity<?> getBotPositions(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId
    ) {
        try {
            verifyBotOwnership(botId, user);

            List<BotPosition> positions = positionRepository.findByBotIdOrderByOpenedAtDesc(botId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("positions", positions);
            response.put("totalPositions", positions.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching positions for bot {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * GET /api/bots/{botId}/positions/open
     */
    @GetMapping("/positions/open")
    public ResponseEntity<?> getOpenPositions(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId
    ) {
        try {
            verifyBotOwnership(botId, user);

            List<BotPosition> openPositions = positionRepository.findByBotIdAndStatus(botId, PositionStatus.OPEN);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("positions", openPositions);
            response.put("count", openPositions.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching open positions for bot {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Performance ───────────────────────────────────────────────────────────

    /**
     * GET /api/bots/{botId}/performance
     */
    @GetMapping("/performance")
    public ResponseEntity<?> getBotPerformance(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId
    ) {
        try {
            verifyBotOwnership(botId, user);

            List<BotPerformanceSnapshot> snapshots = snapshotRepository
                    .findByBotIdOrderBySnapshotTimeDesc(botId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("snapshots", snapshots);
            response.put("count", snapshots.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching performance for bot {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/bots/{botId}/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getBotStats(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId
    ) {
        try {
            verifyBotOwnership(botId, user);

            long totalTrades    = tradeRepository.countByBotId(botId);
            long winningTrades  = tradeRepository.countProfitableTradesByBotId(botId);
            long losingTrades   = tradeRepository.countLosingTradesByBotId(botId);

            var totalRealizedPnl   = tradeRepository.getTotalRealizedPnl(botId);
            var totalUnrealizedPnl = positionRepository.getTotalUnrealizedPnl(botId);
            var avgWin             = tradeRepository.getAverageWin(botId);
            var avgLoss            = tradeRepository.getAverageLoss(botId);
            var largestWin         = tradeRepository.getLargestWin(botId);
            var largestLoss        = tradeRepository.getLargestLoss(botId);

            long openPositions      = positionRepository.countByBotIdAndStatus(botId, PositionStatus.OPEN);
            var openPositionsValue  = positionRepository.getTotalOpenPositionsValue(botId);

            double winRate = totalTrades > 0 ? (winningTrades * 100.0) / totalTrades : 0;

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTrades",       totalTrades);
            stats.put("winningTrades",     winningTrades);
            stats.put("losingTrades",      losingTrades);
            stats.put("winRate",           winRate);
            stats.put("totalRealizedPnl",  totalRealizedPnl);
            stats.put("totalUnrealizedPnl",totalUnrealizedPnl);
            stats.put("averageWin",        avgWin);
            stats.put("averageLoss",       avgLoss);
            stats.put("largestWin",        largestWin);
            stats.put("largestLoss",       largestLoss);
            stats.put("openPositions",     openPositions);
            stats.put("openPositionsValue",openPositionsValue);

            return ResponseEntity.ok(Map.of("success", true, "stats", stats));

        } catch (Exception e) {
            log.error("❌ Error fetching stats for bot {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Conditions ────────────────────────────────────────────────────────────

    /**
     * GET /api/bots/{botId}/conditions
     */
    @GetMapping("/conditions")
    public ResponseEntity<?> getBotConditions(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId
    ) {
        try {
            verifyBotOwnership(botId, user);

            List<BotIndicatorCondition> entryConditions = conditionRepository
                    .findByBotIdAndConditionTypeOrderByConditionOrder(botId, ConditionType.ENTRY);

            List<BotIndicatorCondition> exitConditions = conditionRepository
                    .findByBotIdAndConditionTypeOrderByConditionOrder(botId, ConditionType.EXIT);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "entryConditions", entryConditions,
                    "exitConditions", exitConditions
            ));

        } catch (Exception e) {
            log.error("❌ Error fetching conditions for bot {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}