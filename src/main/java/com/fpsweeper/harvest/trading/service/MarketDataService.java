package com.fpsweeper.harvest.trading.service;

import com.fpsweeper.harvest.trading.MarketDataCache;
import com.fpsweeper.harvest.trading.MarketDataCacheRepository;
import com.fpsweeper.harvest.trading.binance.BinanceApiService;
import com.fpsweeper.harvest.trading.binance.BinanceKlineResponse;
import com.fpsweeper.harvest.trading.binance.CryptoCompareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    @Autowired private BinanceApiService         binanceApiService;
    @Autowired private CryptoCompareService      cryptoCompareService;
    @Autowired private MarketDataCacheRepository marketDataCacheRepository;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Get current price — Binance first, then CryptoCompare, then stale cache.
     */
    public BigDecimal getCurrentPrice(String symbol) {
        try {
            BigDecimal price = binanceApiService.getCurrentPrice(symbol);
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) return price;
        } catch (Exception ignored) {}

        try {
            BigDecimal price = cryptoCompareService.getCurrentPrice(symbol);
            if (price != null && price.compareTo(BigDecimal.ZERO) > 0) return price;
        } catch (Exception ignored) {}

        return marketDataCacheRepository
                .findFirstBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, "1h")
                .map(MarketDataCache::getClosePrice)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get OHLCV candles with a 4-layer fallback:
     *   1. Fresh cache — fastest, no external call
     *   2. Binance      — real-time data
     *   3. CryptoCompare — no geo-restrictions, free tier
     *   4. Stale cache  — keeps bots running even when all APIs are down
     */
    public List<MarketDataCache> getCandles(String symbol, String timeframe, int limit) {

        List<MarketDataCache> cached = marketDataCacheRepository
                .findBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe);

        // ── Layer 1: fresh cache ──────────────────────────────────────────
        if (!cached.isEmpty() && cached.size() >= limit) {
            Instant latestTime = cached.get(0).getOpenTime();
            if (latestTime.plusMillis(cacheMaxAgeMs(timeframe)).isAfter(Instant.now())) {
                return cached.stream().limit(limit).collect(Collectors.toList());
            }
        }

        // ── Layer 2: Binance ──────────────────────────────────────────────
        try {
            List<BinanceKlineResponse> klines = binanceApiService.getKlines(symbol, timeframe, limit);
            if (!klines.isEmpty()) {
                return persistAndReturn(klines, symbol, timeframe, limit);
            }
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            // 451 = geo-restricted — expected in restricted regions, suppress completely
            if (!msg.contains("451")) {
                log.error("❌ Binance error for {} ({}): {}", symbol, timeframe, msg);
            }
        }

        // ── Layer 3: CryptoCompare (no geo-restrictions) ──────────────────
        try {
            List<BinanceKlineResponse> klines = cryptoCompareService.getKlines(symbol, timeframe, limit);
            if (!klines.isEmpty()) {
                log.info("📡 Using CryptoCompare data for {} ({})", symbol, timeframe);
                return persistAndReturn(klines, symbol, timeframe, limit);
            }
        } catch (Exception e) {
            log.error("❌ CryptoCompare error for {} ({}): {}", symbol, timeframe, e.getMessage());
        }

        // ── Layer 4: stale cache — never throw, keep bots alive ───────────
        if (!cached.isEmpty()) {
            log.warn("⚠️ All market data sources failed for {} ({}) — using {} stale candles",
                    symbol, timeframe, Math.min(cached.size(), limit));
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        throw new RuntimeException("No market data available for " + symbol + " (" + timeframe + ")");
    }

    public Optional<MarketDataCache> getLatestCandle(String symbol, String timeframe) {
        return marketDataCacheRepository.findFirstBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe);
    }

    public void clearOldCache(int daysToKeep) {
        Instant cutoff = Instant.now().minusSeconds(daysToKeep * 24L * 3600L);
        marketDataCacheRepository.deleteByOpenTimeBefore(cutoff);
    }

    // ── Internal helpers ───────────────────────────────────────────────────

    private List<MarketDataCache> persistAndReturn(
            List<BinanceKlineResponse> klines, String symbol, String timeframe, int limit) {

        // Batch-fetch all existing candles for this symbol/timeframe in one query
        // to avoid N individual findBySymbolAndTimeframeAndOpenTime calls
        List<MarketDataCache> existing = marketDataCacheRepository
                .findBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe);

        java.util.Map<Instant, MarketDataCache> existingByTime = existing.stream()
                .collect(java.util.stream.Collectors.toMap(
                        MarketDataCache::getOpenTime,
                        c -> c,
                        (a, b) -> a // keep first on duplicate
                ));

        List<MarketDataCache> toSave = klines.stream().map(k -> {
            Instant openTime  = Instant.ofEpochMilli(k.getOpenTime());
            Instant closeTime = Instant.ofEpochMilli(k.getCloseTime());

            // Reuse existing entity (has ID) → triggers UPDATE not INSERT
            MarketDataCache cache = existingByTime.getOrDefault(openTime, new MarketDataCache());

            cache.setSymbol(symbol);
            cache.setTimeframe(timeframe);
            cache.setOpenTime(openTime);
            cache.setCloseTime(closeTime);
            cache.setOpenPrice(k.getOpen());
            cache.setHighPrice(k.getHigh());
            cache.setLowPrice(k.getLow());
            cache.setClosePrice(k.getClose());
            cache.setVolume(k.getVolume());
            cache.setDataSource("BINANCE");
            cache.setIsComplete(true);
            cache.setUpdatedAt(Instant.now());
            return cache;
        }).collect(Collectors.toList());

        try {
            marketDataCacheRepository.saveAll(toSave);
        } catch (Exception e) {
            // If a race condition still causes a duplicate, log and continue —
            // the existing cached data is still valid
            log.warn("⚠️ Could not persist candles for {} ({}): {}", symbol, timeframe, e.getMessage());
        }

        return toSave.stream()
                .sorted((a, b) -> b.getOpenTime().compareTo(a.getOpenTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Max cache age = half the candle interval.
     * Ensures we refresh before the current candle closes.
     */
    private long cacheMaxAgeMs(String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "1m"  -> 30_000L;
            case "3m"  -> 90_000L;
            case "5m"  -> 150_000L;
            case "15m" -> 450_000L;
            case "30m" -> 900_000L;
            case "1h"  -> 1_800_000L;
            case "2h"  -> 3_600_000L;
            case "4h"  -> 7_200_000L;
            case "6h"  -> 10_800_000L;
            case "8h"  -> 14_400_000L;
            case "12h" -> 21_600_000L;
            case "1d"  -> 43_200_000L;
            default    -> 1_800_000L;
        };
    }
}