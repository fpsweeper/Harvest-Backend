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
import java.util.Map;
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
     * Get historical candles with caching
     */
    public List<MarketDataCache> getCandles(String symbol, String timeframe, int limit) {
        List<MarketDataCache> cachedData = marketDataCacheRepository
                .findBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe);

        if (!cachedData.isEmpty() && cachedData.size() >= limit) {
            Instant latestTime = cachedData.get(0).getOpenTime();
            Instant now = Instant.now();

            // Use cache if latest candle is less than 1 hour old
            if (latestTime.plusSeconds(3600).isAfter(now)) {
                log.info("📦 Using cached data for {} ({}): {} candles", symbol, timeframe, cachedData.size());
                return cachedData.stream().limit(limit).collect(Collectors.toList());
            }
        }

        log.info("🔄 Fetching fresh data from Binance for {} ({})...", symbol, timeframe);
        return fetchAndCacheCandles(symbol, timeframe, limit);
    }

    /**
     * Fetch candles from Binance and upsert into cache.
     *
     * FIX: Instead of querying the DB once per candle (200 SELECT calls) inside a
     * stream, we load ALL existing candles for this symbol/timeframe upfront into a
     * Map<openTime, entity>.  Then the stream does an in-memory lookup — no per-row
     * DB call, no duplicate-key race condition on saveAll().
     */
    private List<MarketDataCache> fetchAndCacheCandles(String symbol, String timeframe, int limit) {
        try {
            List<BinanceKlineResponse> klines = binanceApiService.getKlines(symbol, timeframe, limit);

            if (klines.isEmpty()) {
                log.error("❌ No klines returned from Binance for {} ({})", symbol, timeframe);
                return List.of();
            }

            // --- FIX: bulk-load existing rows once, index by openTime ---
            Map<Instant, MarketDataCache> existingByOpenTime = marketDataCacheRepository
                    .findBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe)
                    .stream()
                    .collect(Collectors.toMap(MarketDataCache::getOpenTime, c -> c));

            List<MarketDataCache> cacheEntries = klines.stream().map(kline -> {
                Instant openTime  = Instant.ofEpochMilli(kline.getOpenTime());
                Instant closeTime = Instant.ofEpochMilli(kline.getCloseTime());

                // In-memory lookup — no extra DB query per candle
                MarketDataCache cache = existingByOpenTime.getOrDefault(openTime, null);
                if (cache == null) {
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

            log.info("✅ Upserted {} candles for {} ({})", cacheEntries.size(), symbol, timeframe);
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