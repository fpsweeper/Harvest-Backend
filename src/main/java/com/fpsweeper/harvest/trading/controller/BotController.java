package com.fpsweeper.harvest.trading.controller;

import com.fpsweeper.harvest.trading.dto.BotResponse;
import com.fpsweeper.harvest.trading.dto.CreateBotRequest;
import com.fpsweeper.harvest.trading.dto.UpdateBotRequest;
import com.fpsweeper.harvest.trading.service.BotService;
import com.fpsweeper.harvest.user.Users;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bots")
@CrossOrigin(origins = "*")
public class BotController {

    private static final Logger log = LoggerFactory.getLogger(BotController.class);

    @Autowired
    private BotService botService;

    @PostMapping
    public ResponseEntity<?> createBot(
            @AuthenticationPrincipal Users user,
            @Valid @RequestBody CreateBotRequest request
    ) {
        if (user == null) return unauthorized();

        try {
            log.info("📥 Creating bot: {} for user: {}", request.getName(), user.getId());
            BotResponse bot = botService.createBot(request, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(success("Bot created successfully", "bot", bot));
        } catch (Exception e) {
            log.error("❌ Error creating bot: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserBots(@AuthenticationPrincipal Users user) {
        if (user == null) return unauthorized();

        try {
            List<BotResponse> bots = botService.getUserBots(user.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", bots.size());
            response.put("bots", bots);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error fetching bots: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBotById(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID id
    ) {
        if (user == null) return unauthorized();

        try {
            BotResponse bot = botService.getBotById(id, user.getId());
            return ResponseEntity.ok(success(null, "bot", bot));
        } catch (Exception e) {
            log.error("❌ Error fetching bot {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/start")
    public ResponseEntity<?> startBot(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID id
    ) {
        if (user == null) return unauthorized();

        try {
            log.info("▶️ Starting bot: {} for user: {}", id, user.getId());
            BotResponse bot = botService.startBot(id, user.getId());
            return ResponseEntity.ok(success("Bot started successfully", "bot", bot));
        } catch (Exception e) {
            log.error("❌ Error starting bot {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/pause")
    public ResponseEntity<?> pauseBot(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID id
    ) {
        if (user == null) return unauthorized();

        try {
            log.info("⏸️ Pausing bot: {} for user: {}", id, user.getId());
            BotResponse bot = botService.pauseBot(id, user.getId());
            return ResponseEntity.ok(success("Bot paused successfully", "bot", bot));
        } catch (Exception e) {
            log.error("❌ Error pausing bot {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PutMapping("/{id}/stop")
    public ResponseEntity<?> stopBot(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID id
    ) {
        if (user == null) return unauthorized();

        try {
            log.info("⏹️ Stopping bot: {} for user: {}", id, user.getId());
            BotResponse bot = botService.stopBot(id, user.getId());
            return ResponseEntity.ok(success("Bot stopped successfully", "bot", bot));
        } catch (Exception e) {
            log.error("❌ Error stopping bot {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBot(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID id
    ) {
        if (user == null) return unauthorized();

        try {
            log.info("🗑️ Deleting bot: {} for user: {}", id, user.getId());
            botService.deleteBot(id, user.getId());
            return ResponseEntity.ok(success("Bot deleted successfully", null, null));
        } catch (Exception e) {
            log.error("❌ Error deleting bot {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBot(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID id,
            @RequestBody UpdateBotRequest request
    ) {
        if (user == null) return unauthorized();
        try {
            BotResponse bot = botService.updateBot(id, user.getId(), request);
            return ResponseEntity.ok(success("Bot updated successfully", "bot", bot));
        } catch (Exception e) {
            log.error("❌ Error updating bot {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "error", "Unauthorized"));
    }

    private Map<String, Object> success(String message, String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        if (message != null) map.put("message", message);
        if (key != null && value != null) map.put(key, value);
        return map;
    }

    private Map<String, Object> error(String message) {
        return Map.of("success", false, "error", message);
    }
}