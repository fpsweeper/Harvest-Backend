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
public interface BotPerformanceSnapshotRepository extends JpaRepository<BotPerformanceSnapshot, UUID> {

    // Find all snapshots for a bot (ordered by time)
    List<BotPerformanceSnapshot> findByBotIdOrderBySnapshotTimeDesc(UUID botId);

    // Find snapshots in date range
    List<BotPerformanceSnapshot> findByBotIdAndSnapshotTimeBetweenOrderBySnapshotTime(
            UUID botId, Instant startTime, Instant endTime
    );

    // Find snapshots by type
    List<BotPerformanceSnapshot> findByBotIdAndSnapshotTypeOrderBySnapshotTimeDesc(
            UUID botId, SnapshotType snapshotType
    );

    // Get latest snapshot
    Optional<BotPerformanceSnapshot> findFirstByBotIdOrderBySnapshotTimeDesc(UUID botId);

    // Get snapshot for specific time
    Optional<BotPerformanceSnapshot> findByBotIdAndSnapshotTime(UUID botId, Instant snapshotTime);

    // Delete old snapshots (cleanup)
    @Query("DELETE FROM BotPerformanceSnapshot s WHERE s.snapshotTime < :cutoffTime")
    void deleteSnapshotsOlderThan(@Param("cutoffTime") Instant cutoffTime);

    // Count snapshots for bot
    long countByBotId(UUID botId);
}