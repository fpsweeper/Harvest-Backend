package com.fpsweeper.harvest.trading.service;

import com.fpsweeper.harvest.trading.MarketDataCache;
import com.fpsweeper.harvest.trading.MarketDataCacheRepository;
import com.fpsweeper.harvest.trading.binance.BinanceApiService;
import com.fpsweeper.harvest.trading.binance.BinanceKlineResponse;
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

    @Autowired
    private BinanceApiService binanceApiService;

    @Autowired
    private MarketDataCacheRepository marketDataCacheRepository;

    /**
     * Get current price (always fetch fresh from Binance)
     */
    public BigDecimal getCurrentPrice(String symbol) {
        return binanceApiService.getCurrentPrice(symbol);
    }

    /**
     * Get historical candles with caching.
     *
     * Fix: Previously used a hardcoded 1-hour staleness check which caused
     * the service to always try Binance for older data. If Binance failed
     * (rate limit, network issue on Render), it threw an exception and
     * the frontend got an empty chart. Now:
     * - Staleness threshold scales per timeframe
     * - If Binance fetch fails, stale cache is returned instead of empty
     */
    public List<MarketDataCache> getCandles(String symbol, String timeframe, int limit) {
        List<MarketDataCache> cachedData = marketDataCacheRepository
                .findBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe);

        Instant now = Instant.now();

        if (!cachedData.isEmpty()) {
            Instant latestTime = cachedData.get(0).getOpenTime();

            // Staleness threshold = 2x the timeframe duration
            // e.g. 1h candles are considered fresh for 2 hours
            long stalenessSeconds = timeframeToSeconds(timeframe) * 2;

            if (latestTime.plusSeconds(stalenessSeconds).isAfter(now)) {
                log.info("📦 Using cached data for {} ({}): {} candles", symbol, timeframe, cachedData.size());
                return cachedData.stream().limit(limit).collect(Collectors.toList());
            }

            // Cache is stale — try to refresh from Binance
            log.info("🔄 Cache stale for {} ({}), fetching fresh data...", symbol, timeframe);
            try {
                return fetchAndCacheCandles(symbol, timeframe, limit);
            } catch (Exception e) {
                // Binance unavailable — serve stale cache rather than empty chart
                log.warn("⚠️ Binance fetch failed for {} ({}), serving stale cache ({}): {}",
                        symbol, timeframe, cachedData.size(), e.getMessage());
                return cachedData.stream().limit(limit).collect(Collectors.toList());
            }
        }

        // No cache at all — must fetch from Binance
        log.info("🔄 No cache found for {} ({}), fetching from Binance...", symbol, timeframe);
        return fetchAndCacheCandles(symbol, timeframe, limit);
    }

    /**
     * Returns the candle duration in seconds for a given timeframe.
     * Used to calculate staleness thresholds.
     */
    private long timeframeToSeconds(String timeframe) {
        return switch (timeframe) {
            case "1m"  -> 60L;
            case "5m"  -> 300L;
            case "15m" -> 900L;
            case "30m" -> 1800L;
            case "1h"  -> 3600L;
            case "4h"  -> 14400L;
            case "1d"  -> 86400L;
            default    -> 3600L;
        };
    }

    /**
     * Fetch candles from Binance and cache them
     */
    private List<MarketDataCache> fetchAndCacheCandles(String symbol, String timeframe, int limit) {
        try {
            List<BinanceKlineResponse> klines = binanceApiService.getKlines(symbol, timeframe, limit);

            if (klines.isEmpty()) {
                log.error("❌ No klines returned from Binance for {} ({})", symbol, timeframe);
                return List.of();
            }

            List<MarketDataCache> cacheEntries = klines.stream().map(kline -> {
                Instant openTime  = Instant.ofEpochMilli(kline.getOpenTime());
                Instant closeTime = Instant.ofEpochMilli(kline.getCloseTime());

                Optional<MarketDataCache> existing = marketDataCacheRepository
                        .findBySymbolAndTimeframeAndOpenTime(symbol, timeframe, openTime);

                MarketDataCache cache;
                if (existing.isPresent()) {
                    cache = existing.get();
                } else {
                    cache = new MarketDataCache();
                    cache.setSymbol(symbol);
                    cache.setTimeframe(timeframe);
                    cache.setOpenTime(openTime);
                }

                cache.setCloseTime(closeTime);
                cache.setOpenPrice(kline.getOpen());
                cache.setHighPrice(kline.getHigh());
                cache.setLowPrice(kline.getLow());
                cache.setClosePrice(kline.getClose());
                cache.setVolume(kline.getVolume());
                cache.setDataSource("BINANCE");
                cache.setIsComplete(true);
                cache.setUpdatedAt(Instant.now());

                return cache;
            }).collect(Collectors.toList());

            marketDataCacheRepository.saveAll(cacheEntries);
            log.info("✅ Cached {} candles for {} ({})", cacheEntries.size(), symbol, timeframe);
            return cacheEntries;

        } catch (Exception e) {
            log.error("❌ Error fetching and caching candles: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch market data", e);
        }
    }

    /**
     * Get latest candle (most recent)
     */
    public Optional<MarketDataCache> getLatestCandle(String symbol, String timeframe) {
        return marketDataCacheRepository.findFirstBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe);
    }

    /**
     * Clear old cached data (cleanup)
     */
    public void clearOldCache(int daysToKeep) {
        Instant cutoff = Instant.now().minusSeconds(daysToKeep * 24L * 3600L);
        marketDataCacheRepository.deleteByOpenTimeBefore(cutoff);
        log.info("🗑️ Cleared cached data older than {} days", daysToKeep);
    }
}