package com.fpsweeper.harvest.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolanaWalletRepository extends JpaRepository<SolanaWallets, UUID> {

    Optional<SolanaWallets> findByUserId(UUID userId);

    Optional<SolanaWallets> findByWalletAddress(String walletAddress);

    boolean existsByUserId(UUID userId);

    boolean existsByWalletAddress(String walletAddress);

    void deleteByUserId(UUID userId);

    Optional<SolanaWallets> findByUserIdAndWalletAddress(UUID userId, String walletAddress);

}