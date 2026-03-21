package com.fpsweeper.harvest.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MarketDataCacheRepository extends JpaRepository<MarketDataCache, UUID> {

    // Find candles for symbol and timeframe
    List<MarketDataCache> findBySymbolAndTimeframeOrderByOpenTimeDesc(String symbol, String timeframe);

    // Find candles in date range
    List<MarketDataCache> findBySymbolAndTimeframeAndOpenTimeBetweenOrderByOpenTime(
            String symbol, String timeframe, Instant startTime, Instant endTime
    );

    // Find latest N candles
    @Query("SELECT m FROM MarketDataCache m WHERE m.symbol = :symbol AND m.timeframe = :timeframe ORDER BY m.openTime DESC")
    List<MarketDataCache> findLatestCandles(@Param("symbol") String symbol, @Param("timeframe") String timeframe);

    // Get latest candle
    Optional<MarketDataCache> findFirstBySymbolAndTimeframeOrderByOpenTimeDesc(String symbol, String timeframe);

    // Check if candle exists
    boolean existsBySymbolAndTimeframeAndOpenTime(String symbol, String timeframe, Instant openTime);

    // Find candle by time
    Optional<MarketDataCache> findBySymbolAndTimeframeAndOpenTime(String symbol, String timeframe, Instant openTime);

    // Delete old candles (cleanup)
    void deleteByOpenTimeBefore(Instant cutoffTime);

    // Count cached candles
    long countBySymbolAndTimeframe(String symbol, String timeframe);
}