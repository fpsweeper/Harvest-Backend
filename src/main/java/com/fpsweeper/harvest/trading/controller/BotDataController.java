package com.fpsweeper.harvest.trading.controller;

import com.fpsweeper.harvest.trading.*;
import com.fpsweeper.harvest.user.Users;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/trades")
    public ResponseEntity<?> getBotTrades(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) return unauthorized();

        try {
            verifyOwnership(botId, user.getId());
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
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/positions")
    public ResponseEntity<?> getBotPositions(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId
    ) {
        if (user == null) return unauthorized();

        try {
            verifyOwnership(botId, user.getId());
            List<BotPosition> positions = positionRepository.findByBotIdOrderByOpenedAtDesc(botId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("positions", positions);
            response.put("totalPositions", positions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching positions for bot {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/positions/open")
    public ResponseEntity<?> getOpenPositions(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId
    ) {
        if (user == null) return unauthorized();

        try {
            verifyOwnership(botId, user.getId());
            List<BotPosition> openPositions = positionRepository.findByBotIdAndStatus(botId, PositionStatus.OPEN);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("positions", openPositions);
            response.put("count", openPositions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching open positions for bot {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/performance")
    public ResponseEntity<?> getBotPerformance(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId
    ) {
        if (user == null) return unauthorized();

        try {
            verifyOwnership(botId, user.getId());
            List<BotPerformanceSnapshot> snapshots = snapshotRepository
                    .findByBotIdOrderBySnapshotTimeDesc(botId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("snapshots", snapshots);
            response.put("count", snapshots.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching performance for bot {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getBotStats(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId
    ) {
        if (user == null) return unauthorized();

        try {
            verifyOwnership(botId, user.getId());

            long totalTrades = tradeRepository.countByBotId(botId);
            long winningTrades = tradeRepository.countProfitableTradesByBotId(botId);
            long losingTrades = tradeRepository.countLosingTradesByBotId(botId);
            var totalRealizedPnl = tradeRepository.getTotalRealizedPnl(botId);
            var totalUnrealizedPnl = positionRepository.getTotalUnrealizedPnl(botId);
            var avgWin = tradeRepository.getAverageWin(botId);
            var avgLoss = tradeRepository.getAverageLoss(botId);
            var largestWin = tradeRepository.getLargestWin(botId);
            var largestLoss = tradeRepository.getLargestLoss(botId);
            long openPositions = positionRepository.countByBotIdAndStatus(botId, PositionStatus.OPEN);
            var openPositionsValue = positionRepository.getTotalOpenPositionsValue(botId);
            double winRate = totalTrades > 0 ? (winningTrades * 100.0) / totalTrades : 0;

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalTrades", totalTrades);
            stats.put("winningTrades", winningTrades);
            stats.put("losingTrades", losingTrades);
            stats.put("winRate", winRate);
            stats.put("totalRealizedPnl", totalRealizedPnl);
            stats.put("totalUnrealizedPnl", totalUnrealizedPnl);
            stats.put("averageWin", avgWin);
            stats.put("averageLoss", avgLoss);
            stats.put("largestWin", largestWin);
            stats.put("largestLoss", largestLoss);
            stats.put("openPositions", openPositions);
            stats.put("openPositionsValue", openPositionsValue);

            return ResponseEntity.ok(Map.of("success", true, "stats", stats));
        } catch (Exception e) {
            log.error("❌ Error fetching stats for bot {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/conditions")
    public ResponseEntity<?> getBotConditions(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID botId
    ) {
        if (user == null) return unauthorized();

        try {
            verifyOwnership(botId, user.getId());
            List<BotIndicatorCondition> entryConditions = conditionRepository
                    .findByBotIdAndConditionTypeOrderByConditionOrder(botId, ConditionType.ENTRY);
            List<BotIndicatorCondition> exitConditions = conditionRepository
                    .findByBotIdAndConditionTypeOrderByConditionOrder(botId, ConditionType.EXIT);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("entryConditions", entryConditions);
            response.put("exitConditions", exitConditions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching conditions for bot {}: {}", botId, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void verifyOwnership(UUID botId, UUID userId) {
        botRepository.findByIdAndUserId(botId, userId)
                .orElseThrow(() -> new RuntimeException("Bot not found or access denied"));
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "error", "Unauthorized"));
    }

    private Map<String, Object> error(String message) {
        return Map.of("success", false, "error", message);
    }
}