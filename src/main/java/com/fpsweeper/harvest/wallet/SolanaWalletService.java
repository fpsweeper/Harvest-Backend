package com.fpsweeper.harvest.wallet;

import com.fpsweeper.harvest.auth.exceptions.UserNotFoundException;
import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import com.fpsweeper.harvest.wallet.dto.LinkSolanaWalletRequest;
import com.fpsweeper.harvest.wallet.dto.SolanaWalletResponse;
import com.fpsweeper.harvest.wallet.exceptions.InvalidWalletAddressException;
import com.fpsweeper.harvest.wallet.exceptions.WalletAlreadyLinkedException;
import com.fpsweeper.harvest.wallet.exceptions.WalletNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class SolanaWalletService {

    @Autowired
    private SolanaWalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Pattern SOLANA_ADDRESS_PATTERN =
            Pattern.compile("^[1-9A-HJ-NP-Za-km-z]{32,44}$");

    @Transactional
    public SolanaWalletResponse linkWallet(String email, LinkSolanaWalletRequest request) {
        // Find user
        Users user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        // Validate wallet address format
        if (!isValidSolanaAddress(request.getWalletAddress())) {
            throw new InvalidWalletAddressException();
        }

        // Check if user already has a wallet linked
        if (walletRepository.existsByUserId(user.getId())) {
            throw new WalletAlreadyLinkedException("You already have a wallet linked. Please unlink it first.");
        }

        // Check if wallet is already linked to another account
        if (walletRepository.existsByWalletAddress(request.getWalletAddress())) {
            throw new WalletAlreadyLinkedException("This wallet is already linked to another account");
        }

        // Create new wallet link
        SolanaWallets wallet = new SolanaWallets(user.getId(), request.getWalletAddress());
        wallet.setNickname(request.getNickname());

        // If signature is provided, verify it (simplified - implement proper verification)
        if (request.getSignature() != null && request.getMessage() != null) {
            // TODO: Implement actual signature verification
            wallet.setVerified(true);
            wallet.setLastVerifiedAt(Instant.now());
        }

        walletRepository.save(wallet);

        return new SolanaWalletResponse(wallet);
    }

    @Transactional
    public void unlinkWallet(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        SolanaWallets wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(WalletNotFoundException::new);

        walletRepository.delete(wallet);
    }

    @Transactional
    public void verifyWallet(UUID userId, String walletAddress) {

        SolanaWallets wallet = walletRepository
                .findByUserIdAndWalletAddress(userId, walletAddress)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        wallet.setVerified(true);
        wallet.setLastVerifiedAt(Instant.now());
    }

    public Optional<SolanaWalletResponse> getLinkedWallet(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        System.out.println(user.getEmail() + " jnnnnnnnnnnnnnnnnnnnnnnnnn");
        return walletRepository.findByUserId(user.getId())
                .map(SolanaWalletResponse::new);
    }

    public boolean hasLinkedWallet(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        return walletRepository.existsByUserId(user.getId());
    }

    private boolean isValidSolanaAddress(String address) {
        if (address == null || address.isEmpty()) {
            return false;
        }
        return SOLANA_ADDRESS_PATTERN.matcher(address).matches();
    }
}