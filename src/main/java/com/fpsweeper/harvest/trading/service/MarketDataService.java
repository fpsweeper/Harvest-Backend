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
     * Get current price — always fresh from Binance.
     */
    public BigDecimal getCurrentPrice(String symbol) {
        return binanceApiService.getCurrentPrice(symbol);
    }

    /**
     * Get historical candles with timeframe-aware caching.
     *
     * Cache TTL = half the candle interval, so data is always reasonably current:
     *   5m  → stale after 2.5 min   (was: stale after 60 min — broken for short TFs)
     *   15m → stale after 7.5 min
     *   1h  → stale after 30 min
     *   4h  → stale after 2 h
     *   1d  → stale after 12 h
     */
    public List<MarketDataCache> getCandles(String symbol, String timeframe, int limit) {
        List<MarketDataCache> cachedData = marketDataCacheRepository
                .findBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe);

        if (!cachedData.isEmpty() && cachedData.size() >= limit) {
            Instant latestTime  = cachedData.get(0).getOpenTime();
            long    ttlSeconds  = getCacheTtlSeconds(timeframe);

            if (latestTime.plusSeconds(ttlSeconds).isAfter(Instant.now())) {
                log.info("📦 Using cached data for {} ({}): {} candles", symbol, timeframe, cachedData.size());
                return cachedData.stream().limit(limit).collect(Collectors.toList());
            }
        }

        log.info("🔄 Fetching fresh data from Binance for {} ({})...", symbol, timeframe);
        return fetchAndCacheCandles(symbol, timeframe, limit);
    }

    /**
     * Cache TTL = half the candle duration in seconds.
     */
    private long getCacheTtlSeconds(String timeframe) {
        return switch (timeframe.toLowerCase()) {
            case "1m"  -> 30;
            case "3m"  -> 90;
            case "5m"  -> 150;
            case "15m" -> 450;
            case "30m" -> 900;
            case "1h"  -> 1800;
            case "2h"  -> 3600;
            case "4h"  -> 7200;
            case "6h"  -> 10800;
            case "12h" -> 21600;
            case "1d"  -> 43200;
            case "1w"  -> 302400;
            default    -> 300;
        };
    }

    /**
     * Fetch from Binance and upsert into cache.
     */
    private List<MarketDataCache> fetchAndCacheCandles(String symbol, String timeframe, int limit) {
        try {
            List<BinanceKlineResponse> klines = binanceApiService.getKlines(symbol, timeframe, limit);

            if (klines.isEmpty()) {
                log.error("❌ No klines returned from Binance for {} ({})", symbol, timeframe);
                return returnStaleOrEmpty(symbol, timeframe, limit);
            }

            List<MarketDataCache> cacheEntries = klines.stream().map(kline -> {
                Instant openTime  = Instant.ofEpochMilli(kline.getOpenTime());
                Instant closeTime = Instant.ofEpochMilli(kline.getCloseTime());

                Optional<MarketDataCache> existing = marketDataCacheRepository
                        .findBySymbolAndTimeframeAndOpenTime(symbol, timeframe, openTime);

                MarketDataCache cache = existing.orElseGet(() -> {
                    MarketDataCache c = new MarketDataCache();
                    c.setSymbol(symbol);
                    c.setTimeframe(timeframe);
                    c.setOpenTime(openTime);
                    return c;
                });

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
            log.error("❌ Error fetching candles for {} ({}): {}", symbol, timeframe, e.getMessage());
            return returnStaleOrEmpty(symbol, timeframe, limit);
        }
    }

    private List<MarketDataCache> returnStaleOrEmpty(String symbol, String timeframe, int limit) {
        List<MarketDataCache> stale = marketDataCacheRepository
                .findBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe);
        if (!stale.isEmpty()) {
            log.warn("⚠️  Returning stale cache ({} candles) for {} ({})", stale.size(), symbol, timeframe);
            return stale.stream().limit(limit).collect(Collectors.toList());
        }
        return List.of();
    }

    public Optional<MarketDataCache> getLatestCandle(String symbol, String timeframe) {
        return marketDataCacheRepository.findFirstBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe);
    }

    public void clearOldCache(int daysToKeep) {
        Instant cutoff = Instant.now().minusSeconds(daysToKeep * 24L * 3600L);
        marketDataCacheRepository.deleteByOpenTimeBefore(cutoff);
        log.info("🗑️ Cleared cached data older than {} days", daysToKeep);
    }
}