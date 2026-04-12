package com.fpsweeper.harvest.trading.controller;

import com.fpsweeper.harvest.trading.dto.BotResponse;
import com.fpsweeper.harvest.trading.dto.CreateBotRequest;
import com.fpsweeper.harvest.trading.service.BotService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // TODO: Get from security context
    private static final UUID MOCK_USER_ID = UUID.fromString("392896cc-961c-4f8e-aa46-6f1292710f35");

    /**
     * Create a new bot
     * POST /api/bots
     */
    @PostMapping
    public ResponseEntity<?> createBot(@Valid @RequestBody CreateBotRequest request) {
        try {


            BotResponse bot = botService.createBot(request, MOCK_USER_ID);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bot created successfully");
            response.put("bot", bot);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("❌ Error creating bot: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get all bots for current user
     * GET /api/bots
     */
    @GetMapping
    public ResponseEntity<?> getUserBots() {
        try {
            List<BotResponse> bots = botService.getUserBots(MOCK_USER_ID);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", bots.size());
            response.put("bots", bots);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching bots: {}", e.getMessage(), e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Get bot by ID
     * GET /api/bots/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBotById(@PathVariable UUID id) {
        try {
            BotResponse bot = botService.getBotById(id, MOCK_USER_ID);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("bot", bot);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error fetching bot {}: {}", id, e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    /**
     * Start a bot
     * PUT /api/bots/{id}/start
     */
    @PutMapping("/{id}/start")
    public ResponseEntity<?> startBot(@PathVariable UUID id) {
        try {


            BotResponse bot = botService.startBot(id, MOCK_USER_ID);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bot started successfully");
            response.put("bot", bot);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error starting bot {}: {}", id, e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Pause a bot
     * PUT /api/bots/{id}/pause
     */
    @PutMapping("/{id}/pause")
    public ResponseEntity<?> pauseBot(@PathVariable UUID id) {
        try {


            BotResponse bot = botService.pauseBot(id, MOCK_USER_ID);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bot paused successfully");
            response.put("bot", bot);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error pausing bot {}: {}", id, e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Stop a bot
     * PUT /api/bots/{id}/stop
     */
    @PutMapping("/{id}/stop")
    public ResponseEntity<?> stopBot(@PathVariable UUID id) {
        try {


            BotResponse bot = botService.stopBot(id, MOCK_USER_ID);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bot stopped successfully");
            response.put("bot", bot);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error stopping bot {}: {}", id, e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Delete a bot
     * DELETE /api/bots/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBot(@PathVariable UUID id) {
        try {


            botService.deleteBot(id, MOCK_USER_ID);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Bot deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error deleting bot {}: {}", id, e.getMessage());

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }
    }
}