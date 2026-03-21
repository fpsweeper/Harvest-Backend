package com.fpsweeper.harvest.trading;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BotPointsUsageRepository extends JpaRepository<BotPointsUsage, UUID> {

    // Find usage by bot
    List<BotPointsUsage> findByBotIdOrderByUsageDateDesc(UUID botId);

    // Find usage by user
    List<BotPointsUsage> findByUserIdOrderByUsageDateDesc(UUID userId);

    // Find usage for specific date
    Optional<BotPointsUsage> findByBotIdAndUsageDate(UUID botId, LocalDate usageDate);

    // Find usage in date range
    List<BotPointsUsage> findByBotIdAndUsageDateBetweenOrderByUsageDate(
            UUID botId, LocalDate startDate, LocalDate endDate
    );

    // Get total points consumed by bot
    @Query("SELECT COALESCE(SUM(u.pointsConsumed), 0) FROM BotPointsUsage u WHERE u.botId = :botId")
    BigDecimal getTotalPointsConsumedByBot(@Param("botId") UUID botId);

    // Get total points consumed by user
    @Query("SELECT COALESCE(SUM(u.pointsConsumed), 0) FROM BotPointsUsage u WHERE u.userId = :userId")
    BigDecimal getTotalPointsConsumedByUser(@Param("userId") UUID userId);

    // Get points consumed today by user
    @Query("SELECT COALESCE(SUM(u.pointsConsumed), 0) FROM BotPointsUsage u WHERE u.userId = :userId AND u.usageDate = :today")
    BigDecimal getPointsConsumedToday(@Param("userId") UUID userId, @Param("today") LocalDate today);

    // Delete old usage records (cleanup)
    void deleteByUsageDateBefore(LocalDate cutoffDate);
}