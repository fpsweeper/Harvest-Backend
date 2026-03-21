package com.fpsweeper.harvest.trading.binance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BinanceApiService {

    private static final Logger log = LoggerFactory.getLogger(BinanceApiService.class);

    @Value("${binance.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    public BinanceApiService() {
        this.restTemplate = new RestTemplate();
    }

    public BigDecimal getCurrentPrice(String symbol) {
        try {
            String url = baseUrl + "/api/v3/ticker/price?symbol=" + symbol;
            log.debug("Fetching current price for {} from {}", symbol, url);

            BinancePriceResponse response = restTemplate.getForObject(url, BinancePriceResponse.class);

            if (response != null && response.getPrice() != null) {
                log.info("✅ Current price for {}: ${}", symbol, response.getPrice());
                return response.getPrice();
            } else {
                log.error("❌ Empty response from Binance for {}", symbol);
                throw new RuntimeException("Failed to fetch price for " + symbol);
            }
        } catch (Exception e) {
            log.error("❌ Error fetching price for {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Failed to fetch price from Binance: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<BinanceKlineResponse> getKlines(String symbol, String interval, int limit) {
        try {
            String url = String.format("%s/api/v3/klines?symbol=%s&interval=%s&limit=%d",
                    baseUrl, symbol, interval, limit);

            log.debug("Fetching {} candles for {} ({}) from {}", limit, symbol, interval, url);

            List<List<Object>> rawResponse = restTemplate.getForObject(url, List.class);

            if (rawResponse == null || rawResponse.isEmpty()) {
                log.error("❌ Empty klines response from Binance for {}", symbol);
                return new ArrayList<>();
            }

            List<BinanceKlineResponse> klines = new ArrayList<>();
            for (List<Object> data : rawResponse) {
                klines.add(new BinanceKlineResponse(data));
            }

            log.info("✅ Fetched {} candles for {} ({})", klines.size(), symbol, interval);
            return klines;

        } catch (Exception e) {
            log.error("❌ Error fetching klines for {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Failed to fetch klines from Binance: " + e.getMessage(), e);
        }
    }

    public Binance24hrTickerResponse get24hrTicker(String symbol) {
        try {
            String url = baseUrl + "/api/v3/ticker/24hr?symbol=" + symbol;
            log.debug("Fetching 24hr ticker for {} from {}", symbol, url);

            Binance24hrTickerResponse response = restTemplate.getForObject(url, Binance24hrTickerResponse.class);

            if (response != null) {
                log.info("✅ 24hr ticker for {}: Price=${}, Change={}%",
                        symbol, response.getLastPrice(), response.getPriceChangePercent());
                return response;
            } else {
                log.error("❌ Empty 24hr ticker response from Binance for {}", symbol);
                throw new RuntimeException("Failed to fetch 24hr ticker for " + symbol);
            }
        } catch (Exception e) {
            log.error("❌ Error fetching 24hr ticker for {}: {}", symbol, e.getMessage());
            throw new RuntimeException("Failed to fetch 24hr ticker from Binance: " + e.getMessage(), e);
        }
    }

    public boolean testConnection() {
        try {
            String url = baseUrl + "/api/v3/ping";
            restTemplate.getForObject(url, Object.class);
            log.info("✅ Binance API connection successful");
            return true;
        } catch (Exception e) {
            log.error("❌ Binance API connection failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get all active USDT trading pairs from Binance
     */
    public List<String> getUsdtTradingPairs() {
        try {
            String url = baseUrl + "/api/v3/exchangeInfo";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            List<Map<String, Object>> symbols = (List<Map<String, Object>>) response.getBody().get("symbols");

            return symbols.stream()
                    .filter(s -> "USDT".equals(s.get("quoteAsset"))
                            && "TRADING".equals(s.get("status")))
                    .map(s -> (String) s.get("symbol"))
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            log.error("❌ Error fetching trading pairs: {}", e.getMessage());
            // Return common pairs as fallback
            return List.of("BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
                    "ADAUSDT", "DOGEUSDT", "AVAXUSDT", "DOTUSDT", "MATICUSDT");
        }
    }
}