package com.fpsweeper.harvest.verification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerificationCodes, UUID> {
    Optional<EmailVerificationCodes> findByUserIdAndCodeAndUsedFalse(UUID userId, String code);
    Optional<EmailVerificationCodes> findTopByUserIdAndUsedFalseOrderByCreatedAtDesc(UUID userId);
}
