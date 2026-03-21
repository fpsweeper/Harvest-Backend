package com.fpsweeper.harvest.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BotPositionRepository extends JpaRepository<BotPosition, UUID> {

    // Find all positions for a bot
    List<BotPosition> findByBotIdOrderByOpenedAtDesc(UUID botId);

    // Find open positions
    List<BotPosition> findByBotIdAndStatus(UUID botId, PositionStatus status);

    // Find specific open position for symbol
    Optional<BotPosition> findByBotIdAndSymbolAndStatus(UUID botId, String symbol, PositionStatus status);

    // Count open positions
    long countByBotIdAndStatus(UUID botId, PositionStatus status);

    // Get total value of open positions
    @Query("SELECT COALESCE(SUM(p.currentValue), 0) FROM BotPosition p WHERE p.botId = :botId AND p.status = 'OPEN'")
    BigDecimal getTotalOpenPositionsValue(@Param("botId") UUID botId);

    // Get total unrealized P&L
    @Query("SELECT COALESCE(SUM(p.unrealizedPnl), 0) FROM BotPosition p WHERE p.botId = :botId AND p.status = 'OPEN'")
    BigDecimal getTotalUnrealizedPnl(@Param("botId") UUID botId);

    // Get total realized P&L from closed positions
    @Query("SELECT COALESCE(SUM(p.realizedPnl), 0) FROM BotPosition p WHERE p.botId = :botId AND p.status = 'CLOSED'")
    BigDecimal getTotalRealizedPnl(@Param("botId") UUID botId);

    // Find all open positions (for updating prices)
    @Query("SELECT p FROM BotPosition p WHERE p.status = 'OPEN'")
    List<BotPosition> findAllOpenPositions();
}