package com.fpsweeper.harvest.trading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface BotTradeRepository extends JpaRepository<BotTrade, UUID> {

    // Find all trades for a bot (paginated)
    Page<BotTrade> findByBotIdOrderByExecutedAtDesc(UUID botId, Pageable pageable);

    // Find all trades for a bot (list)
    List<BotTrade> findByBotIdOrderByExecutedAtDesc(UUID botId);

    // Find recent trades (last N)
    List<BotTrade> findTop10ByBotIdOrderByExecutedAtDesc(UUID botId);

    // Count total trades
    long countByBotId(UUID botId);

    // Count profitable trades
    @Query("SELECT COUNT(t) FROM BotTrade t WHERE t.botId = :botId AND t.profitLoss > 0")
    long countProfitableTradesByBotId(@Param("botId") UUID botId);

    // Count losing trades
    @Query("SELECT COUNT(t) FROM BotTrade t WHERE t.botId = :botId AND t.profitLoss < 0")
    long countLosingTradesByBotId(@Param("botId") UUID botId);

    // Get total realized P&L
    @Query("SELECT COALESCE(SUM(t.profitLoss), 0) FROM BotTrade t WHERE t.botId = :botId AND t.profitLoss IS NOT NULL")
    BigDecimal getTotalRealizedPnl(@Param("botId") UUID botId);

    // Find trades by type
    List<BotTrade> findByBotIdAndTradeTypeOrderByExecutedAtDesc(UUID botId, TradeType tradeType);

    // Find trades in date range
    List<BotTrade> findByBotIdAndExecutedAtBetweenOrderByExecutedAtDesc(UUID botId, Instant startDate, Instant endDate);

    // Get average win
    @Query("SELECT AVG(t.profitLoss) FROM BotTrade t WHERE t.botId = :botId AND t.profitLoss > 0")
    BigDecimal getAverageWin(@Param("botId") UUID botId);

    // Get average loss
    @Query("SELECT AVG(t.profitLoss) FROM BotTrade t WHERE t.botId = :botId AND t.profitLoss < 0")
    BigDecimal getAverageLoss(@Param("botId") UUID botId);

    // Get largest win
    @Query("SELECT MAX(t.profitLoss) FROM BotTrade t WHERE t.botId = :botId")
    BigDecimal getLargestWin(@Param("botId") UUID botId);

    // Get largest loss
    @Query("SELECT MIN(t.profitLoss) FROM BotTrade t WHERE t.botId = :botId")
    BigDecimal getLargestLoss(@Param("botId") UUID botId);
}