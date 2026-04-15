package com.fpsweeper.harvest.trading.binance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CryptoCompare public API — no API key required, no geo-restrictions.
 * Used as fallback when Binance returns 451 (restricted region).
 *
 * Docs: https://min-api.cryptocompare.com/documentation
 */
@Service
public class CryptoCompareService {

    private static final Logger log = LoggerFactory.getLogger(CryptoCompareService.class);
    private static final String BASE = "https://min-api.cryptocompare.com/data/v2";

    private final RestTemplate rest = new RestTemplate();

    /**
     * Map Binance timeframe string → CryptoCompare aggregate/endpoint.
     * e.g. "1h" → histohour with aggregate=1
     */
    private record TfConfig(String endpoint, int aggregate) {}

    private TfConfig mapTimeframe(String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "1m"  -> new TfConfig("histominute", 1);
            case "3m"  -> new TfConfig("histominute", 3);
            case "5m"  -> new TfConfig("histominute", 5);
            case "15m" -> new TfConfig("histominute", 15);
            case "30m" -> new TfConfig("histominute", 30);
            case "1h"  -> new TfConfig("histohour",   1);
            case "2h"  -> new TfConfig("histohour",   2);
            case "4h"  -> new TfConfig("histohour",   4);
            case "6h"  -> new TfConfig("histohour",   6);
            case "8h"  -> new TfConfig("histohour",   8);
            case "12h" -> new TfConfig("histohour",   12);
            case "1d"  -> new TfConfig("histoday",    1);
            case "3d"  -> new TfConfig("histoday",    3);
            case "1w"  -> new TfConfig("histoday",    7);
            default    -> new TfConfig("histohour",   1);
        };
    }

    /**
     * Strip quote currency — CryptoCompare uses separate fsym/tsym.
     * BTCUSDT → fsym=BTC, tsym=USDT
     * ETHBTC  → fsym=ETH, tsym=BTC
     */
    private String[] splitSymbol(String symbol) {
        // Common quote currencies, longest first to avoid partial matches
        for (String quote : new String[]{"USDT", "BUSD", "USDC", "BTC", "ETH", "BNB"}) {
            if (symbol.endsWith(quote)) {
                return new String[]{ symbol.substring(0, symbol.length() - quote.length()), quote };
            }
        }
        // Fallback: split at 3 chars
        return new String[]{ symbol.substring(0, 3), symbol.substring(3) };
    }

    /**
     * Fetch OHLCV candles from CryptoCompare.
     * Returns list of BinanceKlineResponse (same interface the rest of the code uses).
     */
    @SuppressWarnings("unchecked")
    public List<BinanceKlineResponse> getKlines(String symbol, String timeframe, int limit) {
        try {
            String[] parts    = splitSymbol(symbol);
            String   fsym     = parts[0];
            String   tsym     = parts[1];
            TfConfig tfConfig = mapTimeframe(timeframe);

            String url = String.format(
                    "%s/%s?fsym=%s&tsym=%s&limit=%d&aggregate=%d",
                    BASE, tfConfig.endpoint(), fsym, tsym, limit, tfConfig.aggregate()
            );

            Map<String, Object> response = rest.getForObject(url, Map.class);
            if (response == null || !"Success".equals(response.get("Response"))) {
                log.warn("⚠️ CryptoCompare returned non-success for {}", symbol);
                return List.of();
            }

            Map<String, Object> data = (Map<String, Object>) response.get("Data");
            List<Map<String, Object>> candles = (List<Map<String, Object>>) data.get("Data");

            List<BinanceKlineResponse> result = new ArrayList<>();
            for (Map<String, Object> c : candles) {
                long   time   = ((Number) c.get("time")).longValue() * 1000L; // seconds → ms
                double open   = ((Number) c.get("open")).doubleValue();
                double high   = ((Number) c.get("high")).doubleValue();
                double low    = ((Number) c.get("low")).doubleValue();
                double close  = ((Number) c.get("close")).doubleValue();
                double volume = ((Number) c.get("volumefrom")).doubleValue();

                // Skip zero candles (padding at start of response)
                if (open == 0 && close == 0) continue;

                BinanceKlineResponse kline = new BinanceKlineResponse();
                kline.setOpenTime(time);
                kline.setCloseTime(time + intervalMs(timeframe));
                kline.setOpen(BigDecimal.valueOf(open));
                kline.setHigh(BigDecimal.valueOf(high));
                kline.setLow(BigDecimal.valueOf(low));
                kline.setClose(BigDecimal.valueOf(close));
                kline.setVolume(BigDecimal.valueOf(volume));
                result.add(kline);
            }

            return result;

        } catch (Exception e) {
            log.error("❌ CryptoCompare error for {}: {}", symbol, e.getMessage());
            return List.of();
        }
    }

    /**
     * Get current price from CryptoCompare.
     */
    @SuppressWarnings("unchecked")
    public BigDecimal getCurrentPrice(String symbol) {
        try {
            String[] parts = splitSymbol(symbol);
            String url = String.format(
                    "https://min-api.cryptocompare.com/data/price?fsym=%s&tsyms=%s",
                    parts[0], parts[1]
            );
            Map<String, Object> response = rest.getForObject(url, Map.class);
            if (response != null && response.containsKey(parts[1])) {
                return new BigDecimal(response.get(parts[1]).toString());
            }
        } catch (Exception e) {
            log.error("❌ CryptoCompare price error for {}: {}", symbol, e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private long intervalMs(String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "1m"  -> 60_000L;
            case "3m"  -> 180_000L;
            case "5m"  -> 300_000L;
            case "15m" -> 900_000L;
            case "30m" -> 1_800_000L;
            case "1h"  -> 3_600_000L;
            case "2h"  -> 7_200_000L;
            case "4h"  -> 14_400_000L;
            case "6h"  -> 21_600_000L;
            case "8h"  -> 28_800_000L;
            case "12h" -> 43_200_000L;
            case "1d"  -> 86_400_000L;
            case "3d"  -> 259_200_000L;
            case "1w"  -> 604_800_000L;
            default    -> 3_600_000L;
        };
    }
}