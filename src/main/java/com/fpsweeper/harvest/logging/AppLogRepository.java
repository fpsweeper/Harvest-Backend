package com.fpsweeper.harvest.logging;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AppLogRepository extends JpaRepository<AppLog, UUID> {

    /** All logs, newest first */
    Page<AppLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Filter by level */
    Page<AppLog> findByLevelOrderByCreatedAtDesc(String level, Pageable pageable);

    /** Full-text filter: level + message/logger search */
    @Query("""
        SELECT l FROM AppLog l
        WHERE (:level IS NULL OR l.level = :level)
          AND (:search IS NULL OR :search = ''
               OR LOWER(l.message)    LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(l.loggerName) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY l.createdAt DESC
        """)
    Page<AppLog> findFiltered(
            @Param("level")  String level,
            @Param("search") String search,
            Pageable pageable
    );

    /** Time-range filter — used for "last N minutes" quick filters */
    @Query("""
        SELECT l FROM AppLog l
        WHERE (:level IS NULL OR l.level = :level)
          AND l.createdAt >= :since
          AND (:search IS NULL OR :search = ''
               OR LOWER(l.message)    LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(l.loggerName) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY l.createdAt DESC
        """)
    Page<AppLog> findFilteredSince(
            @Param("level")  String level,
            @Param("search") String search,
            @Param("since")  Instant since,
            Pageable pageable
    );

    /** Delete logs older than a given timestamp (for cleanup) */
    @Modifying
    @Transactional
    @Query("DELETE FROM AppLog l WHERE l.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    /** Count by level — used for the summary badges */
    long countByLevel(String level);

    /** Count errors in last 24h */
    @Query("SELECT COUNT(l) FROM AppLog l WHERE l.level IN ('ERROR','WARN') AND l.createdAt >= :since")
    long countErrorsSince(@Param("since") Instant since);
}