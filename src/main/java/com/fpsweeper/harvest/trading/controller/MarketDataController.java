package com.fpsweeper.harvest.trading.controller;

import com.fpsweeper.harvest.trading.MarketDataCache;
import com.fpsweeper.harvest.trading.binance.BinanceApiService;
import com.fpsweeper.harvest.trading.service.MarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/market-data")
@CrossOrigin(origins = "*")
public class MarketDataController {

    @Autowired
    private BinanceApiService binanceApiService;

    @Autowired
    private MarketDataService marketDataService;

    /**
     * Test endpoint - Get current price
     * GET /api/market-data/price/BTCUSDT
     */
    @GetMapping("/price/{symbol}")
    public ResponseEntity<Map<String, Object>> getCurrentPrice(@PathVariable String symbol) {
        try {
            BigDecimal price = binanceApiService.getCurrentPrice(symbol);

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol);
            response.put("price", price);
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Test endpoint - Get historical candles
     * GET /api/market-data/candles/BTCUSDT?timeframe=1h&limit=100
     */
    @GetMapping("/candles/{symbol}")
    public ResponseEntity<?> getCandles(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1h") String timeframe,
            @RequestParam(defaultValue = "100") int limit
    ) {
        try {
            List<MarketDataCache> candles = marketDataService.getCandles(symbol, timeframe, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("symbol", symbol);
            response.put("timeframe", timeframe);
            response.put("count", candles.size());
            response.put("candles", candles);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Test Binance API connection
     * GET /api/market-data/test
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        boolean connected = binanceApiService.testConnection();

        Map<String, Object> response = new HashMap<>();
        response.put("binanceConnected", connected);
        response.put("message", connected ? "✅ Connected to Binance API" : "❌ Failed to connect to Binance API");

        return ResponseEntity.ok(response);
    }

    /**
     * Get 24hr ticker stats
     * GET /api/market-data/ticker/BTCUSDT
     */
    @GetMapping("/ticker/{symbol}")
    public ResponseEntity<?> get24hrTicker(@PathVariable String symbol) {
        try {
            var ticker = binanceApiService.get24hrTicker(symbol);
            return ResponseEntity.ok(ticker);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Add this method to your existing MarketDataController.java

    /**
     * Get all available USDT trading pairs from Binance
     * GET /api/market-data/pairs
     */
    @GetMapping("/pairs")
    public ResponseEntity<?> getTradingPairs() {
        try {
            List<String> pairs = binanceApiService.getUsdtTradingPairs();

            Map<String, Object> response = new HashMap<>();
            response.put("pairs", pairs);
            response.put("count", pairs.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}