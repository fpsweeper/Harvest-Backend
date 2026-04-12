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
     * Get historical candles with caching
     * @param symbol Trading pair (BTCUSDT)
     * @param timeframe Interval (1h, 4h, 1d)
     * @param limit Number of candles
     * @return List of cached market data
     */
    public List<MarketDataCache> getCandles(String symbol, String timeframe, int limit) {
        // First, try to get from cache
        List<MarketDataCache> cachedData = marketDataCacheRepository
                .findBySymbolAndTimeframeOrderByOpenTimeDesc(symbol, timeframe);

        // If we have enough cached data and it's recent, use it
        if (!cachedData.isEmpty() && cachedData.size() >= limit) {
            Instant latestTime = cachedData.get(0).getOpenTime();
            Instant now = Instant.now();

            // If latest data is less than 1 hour old, use cache
            if (latestTime.plusSeconds(3600).isAfter(now)) {

                return cachedData.stream().limit(limit).collect(Collectors.toList());
            }
        }

        // Otherwise, fetch fresh data from Binance

        return fetchAndCacheCandles(symbol, timeframe, limit);
    }

    /**
     * Fetch candles from Binance and cache them
     */
    private List<MarketDataCache> fetchAndCacheCandles(String symbol, String timeframe, int limit) {
        try {
            // Fetch from Binance
            List<BinanceKlineResponse> klines = binanceApiService.getKlines(symbol, timeframe, limit);

            if (klines.isEmpty()) {

                return List.of();
            }

            // Convert to MarketDataCache and save
            List<MarketDataCache> cacheEntries = klines.stream().map(kline -> {
                Instant openTime = Instant.ofEpochMilli(kline.getOpenTime());
                Instant closeTime = Instant.ofEpochMilli(kline.getCloseTime());

                // Check if already exists
                Optional<MarketDataCache> existing = marketDataCacheRepository
                        .findBySymbolAndTimeframeAndOpenTime(symbol, timeframe, openTime);

                MarketDataCache cache;
                if (existing.isPresent()) {
                    // Update existing
                    cache = existing.get();
                } else {
                    // Create new
                    cache = new MarketDataCache();
                    cache.setSymbol(symbol);
                    cache.setTimeframe(timeframe);
                    cache.setOpenTime(openTime);
                }

                // Set/update candle data
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

            // Save all to database
            marketDataCacheRepository.saveAll(cacheEntries);



            return cacheEntries;

        } catch (Exception e) {

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

    }
}