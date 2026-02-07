package com.fpsweeper.harvest.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OAuthLinkingTokenRepository extends JpaRepository<OAuthLinkingToken, String> {
    Optional<OAuthLinkingToken> findByToken(String token);
    void deleteByExpiresAtBefore(Instant now);
}
