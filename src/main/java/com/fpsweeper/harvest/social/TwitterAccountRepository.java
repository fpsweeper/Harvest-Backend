package com.fpsweeper.harvest.social;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TwitterAccountRepository extends JpaRepository<TwitterAccounts, UUID> {

    Optional<TwitterAccounts> findByUserId(UUID userId);

    Optional<TwitterAccounts> findByTwitterId(String twitterId);

    boolean existsByUserId(UUID userId);

    boolean existsByTwitterId(String twitterId);

    void deleteByUserId(UUID userId);
}