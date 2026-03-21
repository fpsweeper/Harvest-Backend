package com.fpsweeper.harvest.trading.controller;

import com.fpsweeper.harvest.trading.service.IndicatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @Autowired
    private IndicatorService indicatorService;

    /**
     * Test indicator calculation
     * GET /api/test/indicators/{symbol}?timeframe=1h
     */
    @GetMapping("/indicators/{symbol}")
    public ResponseEntity<?> testIndicators(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1h") String timeframe
    ) {
        try {
            Map<String, BigDecimal> indicators = indicatorService.calculateIndicators(symbol, timeframe);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("symbol", symbol);
            response.put("timeframe", timeframe);
            response.put("indicators", indicators);
            response.put("count", indicators.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Health check
     * GET /api/test/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", "healthy");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }
}