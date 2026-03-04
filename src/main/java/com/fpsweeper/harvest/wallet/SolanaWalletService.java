package com.fpsweeper.harvest.wallet;

import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import com.fpsweeper.harvest.wallet.dto.LinkSolanaWalletRequest;
import com.fpsweeper.harvest.wallet.dto.SolanaWalletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SolanaWalletService {

    @Autowired
    private SolanaWalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Link wallet - backward compatible (defaults to SOLANA)
     */
    @Transactional
    public SolanaWalletResponse linkWallet(String email, LinkSolanaWalletRequest request) {
        return linkWallet(email, request, "SOLANA");
    }

    /**
     * Link wallet - multi-chain version
     */
    @Transactional
    public SolanaWalletResponse linkWallet(String email, LinkSolanaWalletRequest request, String chain) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String normalizedChain = chain.toUpperCase();
        String normalizedAddress = normalizeAddress(normalizedChain, request.getWalletAddress());

        // Check if this wallet is already linked to another user
        Optional<UserWallet> existingWallet = walletRepository.findByWalletAddress(normalizedAddress);
        if (existingWallet.isPresent()) {
            if (!existingWallet.get().getUserId().equals(user.getId())) {
                throw new RuntimeException("This wallet is already linked to another account");
            }
            // Already linked to this user - return it
            return new SolanaWalletResponse(existingWallet.get());
        }

        // Check if user already has a wallet for this chain
        Optional<UserWallet> userWallet = walletRepository.findByUserIdAndChain(user.getId(), normalizedChain);
        if (userWallet.isPresent()) {
            // Update existing wallet
            UserWallet wallet = userWallet.get();
            wallet.setWalletAddress(normalizedAddress);
            wallet.setLinkedAt(Instant.now());
            UserWallet saved = walletRepository.save(wallet);

            System.out.println("✅ Wallet updated: " + user.getEmail() + " → " + normalizedAddress + " (" + normalizedChain + ")");
            return new SolanaWalletResponse(saved);
        }

        // Create new wallet link
        UserWallet wallet = new UserWallet();
        wallet.setUserId(user.getId());
        wallet.setChain(normalizedChain);
        wallet.setWalletAddress(normalizedAddress);
        wallet.setIsVerified(false);
        wallet.setIsPrimary(true);

        UserWallet saved = walletRepository.save(wallet);

        System.out.println("✅ Wallet linked: " + user.getEmail() + " → " + normalizedAddress + " (" + normalizedChain + ")");

        return new SolanaWalletResponse(saved);
    }

    /**
     * Unlink wallet - backward compatible
     */
    @Transactional
    public void unlinkWallet(String email) {
        unlinkWallet(email, "SOLANA");
    }

    /**
     * Unlink wallet - multi-chain version
     */
    @Transactional
    public void unlinkWallet(String email, String chain) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<UserWallet> wallet = walletRepository.findByUserIdAndChain(user.getId(), chain.toUpperCase());
        wallet.ifPresent(w -> {
            walletRepository.delete(w);
            System.out.println("✅ Wallet unlinked: " + email + " (" + chain + ")");
        });
    }

    /**
     * Check if user has linked wallet - backward compatible
     */
    public boolean hasLinkedWallet(String email) {
        return hasLinkedWallet(email, "SOLANA");
    }

    /**
     * Check if user has linked wallet - multi-chain version
     */
    public boolean hasLinkedWallet(String email, String chain) {
        Users user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        return walletRepository.existsByUserIdAndChain(user.getId(), chain.toUpperCase());
    }

    /**
     * Get user's wallet - backward compatible (returns first wallet found)
     */
    public Optional<UserWallet> getUserWallet(UUID userId) {
        return walletRepository.findByUserId(userId);
    }

    /**
     * Get user's wallet for specific chain
     */
    public Optional<UserWallet> getUserWallet(UUID userId, String chain) {
        return walletRepository.findByUserIdAndChain(userId, chain.toUpperCase());
    }

    /**
     * Get all user's wallets (all chains)
     */
    public List<UserWallet> getAllUserWallets(UUID userId) {
        return walletRepository.findAllByUserId(userId);
    }

    /**
     * Find wallet by address
     */
    public Optional<UserWallet> findByWalletAddress(String walletAddress) {
        return walletRepository.findByWalletAddress(walletAddress);
    }

    /**
     * Verify wallet ownership
     */
    @Transactional
    public void verifyWallet(UUID userId, String walletAddress) {
        UserWallet wallet = walletRepository.findByWalletAddress(walletAddress)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        if (!wallet.getUserId().equals(userId)) {
            throw new RuntimeException("Wallet does not belong to this user");
        }

        wallet.setIsVerified(true);
        wallet.setLastVerifiedAt(Instant.now());
        walletRepository.save(wallet);

        System.out.println("✅ Wallet verified: " + walletAddress);
    }

    /**
     * Normalize wallet address format
     */
    private String normalizeAddress(String chain, String address) {
        if ("ARBITRUM".equals(chain)) {
            // EVM addresses: lowercase
            return address.toLowerCase();
        }
        // Solana: case-sensitive, return as-is
        return address.trim();
    }
}