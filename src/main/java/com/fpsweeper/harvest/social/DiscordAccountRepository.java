package com.fpsweeper.harvest.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscordAccountRepository extends JpaRepository<DiscordAccounts, UUID> {
    Optional<DiscordAccounts> findByUserId(UUID userId);
    Optional<DiscordAccounts> findByDiscordId(String discordId);
    boolean existsByUserId(UUID userId);
    boolean existsByDiscordId(String discordId);
}
