package com.fpsweeper.harvest.points;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class PendingDepositService {

    @Autowired
    private PendingDepositRepository pendingDepositRepository;

    @Autowired
    private SupportedChainRepository chainRepository;

    @Autowired
    private DepositService depositService;

    /**
     * Create a new pending deposit session (or return existing one)
     */
    @Transactional
    public PendingDeposit createPendingDeposit(UUID userId, String chain, BigDecimal baseAmount) {

        Optional<PendingDeposit> existing = findExistingPendingDeposit(userId, chain, baseAmount);

        if (existing.isPresent()) {
            PendingDeposit existingDeposit = existing.get();

            // ✅ Check if it's recent (less than 5 minutes old)
            long minutesSinceCreation = java.time.Duration.between(
                    existingDeposit.getCreatedAt(),
                    Instant.now()
            ).toMinutes();

            if (minutesSinceCreation < 5) {
                // Still fresh - return existing
                System.out.println("♻️ Returning recent pending deposit (created " + minutesSinceCreation + "m ago)");
                return existingDeposit;
            } else {
                // Old - delete and create new
                System.out.println("🗑️ Deleting old pending deposit and creating new one");
                pendingDepositRepository.delete(existingDeposit);
            }
        }

        if (existing.isPresent()) {
            PendingDeposit existingDeposit = existing.get();
            System.out.println("♻️ Returning existing pending deposit:");
            System.out.println("   ID: " + existingDeposit.getId());
            System.out.println("   Exact Amount: $" + existingDeposit.getExactAmount());
            System.out.println("   Created: " + existingDeposit.getCreatedAt());
            return existingDeposit;
        }

        // Get chain info
        SupportedChain supportedChain = chainRepository.findByChainName(chain.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Chain not supported: " + chain));

        // Validate minimum
        if (baseAmount.compareTo(supportedChain.getMinDepositUsd()) < 0) {
            throw new RuntimeException("Minimum deposit is $" + supportedChain.getMinDepositUsd());
        }

        // Generate security amount
        BigDecimal securityAmount = generateSecurityAmount();
        BigDecimal exactAmount = baseAmount.add(securityAmount);

        // Calculate points
        BigDecimal points = baseAmount.multiply(depositService.getConversionRate())
                .setScale(2, RoundingMode.HALF_UP);

        // Create pending deposit
        PendingDeposit pending = new PendingDeposit();
        pending.setUserId(userId);
        pending.setChain(chain.toUpperCase());
        pending.setBaseAmount(baseAmount);
        pending.setSecurityAmount(securityAmount);
        pending.setExactAmount(exactAmount);
        pending.setPointsToReceive(points);
        pending.setPlatformWallet(supportedChain.getPlatformWalletAddress());
        pending.setTokenAddress(supportedChain.getUsdcTokenAddress());

        PendingDeposit saved = pendingDepositRepository.save(pending);

        System.out.println("💾 Created NEW pending deposit:");
        System.out.println("   ID: " + saved.getId());
        System.out.println("   User: " + userId);
        System.out.println("   Chain: " + chain);
        System.out.println("   Base Amount: $" + baseAmount);
        System.out.println("   Security Amount: $" + securityAmount);
        System.out.println("   Exact Amount: $" + exactAmount);
        System.out.println("   Expires: " + saved.getExpiresAt());

        return saved;
    }

    /**
     * Find existing active pending deposit for user/chain/amount
     */
    private Optional<PendingDeposit> findExistingPendingDeposit(
            UUID userId,
            String chain,
            BigDecimal baseAmount
    ) {
        List<PendingDeposit> pending = pendingDepositRepository
                .findByUserIdAndSubmittedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        userId,
                        Instant.now()
                );

        // Find matching chain and base amount
        return pending.stream()
                .filter(p -> p.getChain().equalsIgnoreCase(chain))
                .filter(p -> p.getBaseAmount().compareTo(baseAmount) == 0)
                .findFirst();
    }

    /**
     * Get active pending deposits for a user
     */
    public List<PendingDeposit> getActivePendingDeposits(UUID userId) {
        return pendingDepositRepository.findByUserIdAndSubmittedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                userId,
                Instant.now()
        );
    }

    /**
     * Get a specific pending deposit
     */
    public Optional<PendingDeposit> getPendingDeposit(UUID id, UUID userId) {
        return pendingDepositRepository.findByIdAndUserId(id, userId);
    }

    /**
     * Mark pending deposit as submitted
     */
    @Transactional
    public void markAsSubmitted(UUID pendingDepositId, String transactionHash) {
        PendingDeposit pending = pendingDepositRepository.findById(pendingDepositId)
                .orElseThrow(() -> new RuntimeException("Pending deposit not found"));

        pending.setSubmitted(true);
        pending.setTransactionHash(transactionHash);
        pendingDepositRepository.save(pending);

        System.out.println("✅ Marked pending deposit as submitted: " + pendingDepositId);
    }

    /**
     * Cancel a pending deposit
     */
    @Transactional
    public void cancelPendingDeposit(UUID id, UUID userId) {
        PendingDeposit pending = pendingDepositRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Pending deposit not found"));

        pendingDepositRepository.delete(pending);

        System.out.println("🗑️ Cancelled pending deposit: " + id);
    }

    /**
     * Cleanup expired pending deposits (runs daily)
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 2 AM daily
    @Transactional
    public void cleanupExpiredPendingDeposits() {
        List<PendingDeposit> expired = pendingDepositRepository
                .findBySubmittedFalseAndExpiresAtBefore(Instant.now());

        if (!expired.isEmpty()) {
            pendingDepositRepository.deleteAll(expired);
            System.out.println("🧹 Cleaned up " + expired.size() + " expired pending deposits");
        }
    }

    /**
     * Generate random security amount
     */
    private BigDecimal generateSecurityAmount() {
        Random random = new Random();
        int cents = random.nextInt(100) + 1; // 1 to 100 cents
        return new BigDecimal(cents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}