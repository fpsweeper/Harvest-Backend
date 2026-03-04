package com.fpsweeper.harvest.points;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PointTransactionRepository extends JpaRepository<PointTransaction, UUID> {

    List<PointTransaction> findByUserId(UUID userId);

    Page<PointTransaction> findByUserId(UUID userId, Pageable pageable);

    Page<PointTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<PointTransaction> findByType(String type);

    List<PointTransaction> findByUserIdAndType(UUID userId, String type);
}