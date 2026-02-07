package com.fpsweeper.harvest.passwordreset;

import com.fpsweeper.harvest.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordResetCodes, UUID> {

    Optional<PasswordResetCodes> findByCodeAndUsedFalseAndExpiresAtAfter(String code, Instant now);
    Optional<PasswordResetCodes> findByUserIdAndUsedFalseAndExpiresAtAfter(UUID userId, Instant now);
    void deleteByUserId(UUID userId);
}
