package com.fpsweeper.harvest.points;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PendingDepositRepository extends JpaRepository<PendingDeposit, UUID> {

    /**
     * Find active (non-submitted, non-expired) pending deposits for a user
     */
    List<PendingDeposit> findByUserIdAndSubmittedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId,
            Instant now
    );

    /**
     * Find a specific pending deposit by ID and user
     */
    Optional<PendingDeposit> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Find all expired pending deposits
     */
    List<PendingDeposit> findBySubmittedFalseAndExpiresAtBefore(Instant now);

    /**
     * Delete expired pending deposits
     */
    void deleteByExpiresAtBefore(Instant now);
}