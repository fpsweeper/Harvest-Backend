package com.fpsweeper.harvest.trading.controller;

import com.fpsweeper.harvest.trading.scheduler.BotExecutionScheduler;
import com.fpsweeper.harvest.user.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bot-execution")
@CrossOrigin(origins = "*")
public class BotExecutionController {

    @Autowired
    private BotExecutionScheduler botExecutionScheduler;

    /**
     * Manually trigger bot execution (for testing only — remove or protect in production)
     * POST /api/bot-execution/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerExecution(
            @AuthenticationPrincipal Users user
    ) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "error", "Unauthorized"));
        }

        try {
            botExecutionScheduler.executeBotsManually();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Bot execution triggered successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}