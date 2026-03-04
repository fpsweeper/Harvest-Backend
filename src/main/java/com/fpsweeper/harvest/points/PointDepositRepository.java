package com.fpsweeper.harvest.points;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}