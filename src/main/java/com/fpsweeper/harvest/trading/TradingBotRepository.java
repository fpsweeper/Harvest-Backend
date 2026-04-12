package com.fpsweeper.harvest.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TradingBotRepository extends JpaRepository<TradingBot, UUID> {

    List<TradingBot> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<TradingBot> findByStatus(BotStatus status);

    List<TradingBot> findByUserIdAndStatus(UUID userId, BotStatus status);

    /**
     * FIX: Original query — "nextExecutionTime <= :now" — silently excludes rows
     * where nextExecutionTime IS NULL (standard SQL three-value logic).
     * Bots that were just started via startBot() have nextExecutionTime = null
     * and would never be picked up.
     *
     * Fixed query treats NULL as "execute immediately".
     */
    @Query("SELECT b FROM TradingBot b WHERE b.status = com.fpsweeper.harvest.trading.BotStatus.SIMULATING " +
            "AND (b.nextExecutionTime IS NULL OR b.nextExecutionTime <= :now)")
    List<TradingBot> findBotsReadyForExecution(@Param("now") Instant now);

    List<TradingBot> findByUserIdAndTradingPair(UUID userId, String tradingPair);

    long countByUserIdAndStatus(UUID userId, BotStatus status);

    Optional<TradingBot> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT b FROM TradingBot b WHERE b.status IN " +
            "(com.fpsweeper.harvest.trading.BotStatus.SIMULATING, com.fpsweeper.harvest.trading.BotStatus.PAUSED)")
    List<TradingBot> findAllActiveOrPaused();

    @Query("SELECT b FROM TradingBot b WHERE b.status = com.fpsweeper.harvest.trading.BotStatus.STOPPED " +
            "AND b.stoppedAt < :cutoffTime")
    List<TradingBot> findStoppedBotsBefore(@Param("cutoffTime") Instant cutoffTime);

    long countByStatus(BotStatus status);

    /**
     * Total virtual credit currently allocated to a user across all non-deleted bots.
     * Uses initialBalance because currentBalance fluctuates during simulation,
     * but the credit the platform granted is always tracked by initialBalance.
     *
     * Returns 0 if the user has no virtual credit bots yet (COALESCE handles NULL).
     */
    @Query("SELECT COALESCE(SUM(b.initialBalance), 0) FROM TradingBot b " +
            "WHERE b.userId = :userId " +
            "AND b.virtualCredit = true " +
            "AND b.status <> com.fpsweeper.harvest.trading.BotStatus.DELETED")
    BigDecimal sumVirtualCreditByUserId(@Param("userId") UUID userId);
}