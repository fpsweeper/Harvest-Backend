package com.fpsweeper.harvest.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SolanaWalletRepository extends JpaRepository<UserWallet, UUID> {

    Optional<UserWallet> findByUserId(UUID userId);

    Optional<UserWallet> findByWalletAddress(String walletAddress);

    boolean existsByUserId(UUID userId);

    boolean existsByWalletAddress(String walletAddress);

    void deleteByUserId(UUID userId);

    Optional<UserWallet> findByUserIdAndWalletAddress(UUID userId, String walletAddress);

    Optional<UserWallet> findByUserIdAndChain(UUID userId, String chain);
    List<UserWallet> findAllByUserId(UUID userId);
    Optional<UserWallet> findByWalletAddressAndChain(String walletAddress, String chain);
    boolean existsByUserIdAndChain(UUID userId, String chain);


}