package com.fpsweeper.harvest.points;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PointDepositRepository extends JpaRepository<PointDeposit, UUID> {

    Optional<PointDeposit> findByTransactionHash(String transactionHash);

    boolean existsByTransactionHash(String transactionHash);

    List<PointDeposit> findByUserId(UUID userId);

    Page<PointDeposit> findByUserId(UUID userId, Pageable pageable);

    List<PointDeposit> findByStatus(String status);

    Page<PointDeposit> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<PointDeposit> findByStatusOrderBySubmittedAtAsc(String status);

    Page<PointDeposit> findByStatusOrderBySubmittedAtDesc(String status, Pageable pageable);
    Page<PointDeposit> findAllByOrderBySubmittedAtDesc(Pageable pageable);
    long countByUserId(UUID userId);
    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(d.amountUsd), 0) FROM PointDeposit d WHERE d.status = 'CONFIRMED'")
    Optional<BigDecimal> sumConfirmedAmounts();

    @Query("SELECT COALESCE(SUM(d.pointsIssued), 0) FROM PointDeposit d WHERE d.status = 'CONFIRMED'")
    Optional<BigDecimal> sumConfirmedPoints();
}