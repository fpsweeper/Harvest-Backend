package com.fpsweeper.harvest.points;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPointsRepository extends JpaRepository<UserPoints, UUID> {

    Optional<UserPoints> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    // Use @Query annotation with @Lock for pessimistic locking
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT up FROM UserPoints up WHERE up.userId = :userId")
    Optional<UserPoints> findByUserIdWithLock(@Param("userId") UUID userId);
}