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

    @Autowired private PendingDepositRepository pendingDepositRepository;
    @Autowired private SupportedChainRepository chainRepository;
    @Autowired private DepositService depositService;

    // ✅ Inject packages repository for correct points lookup
    @Autowired private PointsPackageRepository packagesRepository;

    /**
     * Look up points for a given USD base amount from the points_packages table.
     * Falls back to flat rate if no package matches.
     */
    private BigDecimal resolvePoints(BigDecimal baseAmount) {
        return packagesRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .filter(p -> p.getPriceUsd().compareTo(baseAmount) == 0)
                .map(PointsPackage::getPoints)
                .findFirst()
                .orElseGet(() -> {
                    System.out.println("⚠️ No package found for $" + baseAmount + " — using flat rate fallback");
                    return baseAmount.multiply(depositService.getConversionRate())
                            .setScale(2, RoundingMode.HALF_UP);
                });
    }

    /**
     * Create a new pending deposit session (or return existing one).
     *
     * Fix: the original code checked existing.isPresent() twice — after deleting
     * the stale deposit it fell into the second check and returned the deleted record.
     * Now uses a clean boolean flag to decide whether to create a new one.
     */
    @Transactional
    public PendingDeposit createPendingDeposit(UUID userId, String chain, BigDecimal baseAmount) {

        Optional<PendingDeposit> existing = findExistingPendingDeposit(userId, chain, baseAmount);

        if (existing.isPresent()) {
            PendingDeposit existingDeposit = existing.get();
            long minutesSinceCreation = java.time.Duration.between(
                    existingDeposit.getCreatedAt(), Instant.now()
            ).toMinutes();

            if (minutesSinceCreation < 5) {
                // Still fresh — reuse it
                System.out.println("♻️ Returning recent pending deposit (created " + minutesSinceCreation + "m ago)");
                return existingDeposit;
            } else {
                // Stale — delete it and fall through to create a new one
                System.out.println("🗑️ Deleting stale pending deposit, creating fresh one");
                pendingDepositRepository.delete(existingDeposit);
                // ✅ Do NOT check existing.isPresent() again — it's already deleted
            }
        }

        // ── Create new pending deposit ──────────────────────────────────────

        SupportedChain supportedChain = chainRepository.findByChainName(chain.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Chain not supported: " + chain));

        if (baseAmount.compareTo(supportedChain.getMinDepositUsd()) < 0)
            throw new RuntimeException("Minimum deposit is $" + supportedChain.getMinDepositUsd());

        BigDecimal securityAmount = generateSecurityAmount();
        BigDecimal exactAmount    = baseAmount.add(securityAmount);

        // ✅ Use package-based points — NOT flat rate
        BigDecimal points = resolvePoints(baseAmount);

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
        System.out.println("   ID: "              + saved.getId());
        System.out.println("   User: "            + userId);
        System.out.println("   Chain: "           + chain);
        System.out.println("   Base Amount: $"    + baseAmount);
        System.out.println("   Security Amount: $"+ securityAmount);
        System.out.println("   Exact Amount: $"   + exactAmount);
        System.out.println("   Points: "          + points);
        System.out.println("   Expires: "         + saved.getExpiresAt());

        return saved;
    }

    private Optional<PendingDeposit> findExistingPendingDeposit(UUID userId, String chain, BigDecimal baseAmount) {
        List<PendingDeposit> pending = pendingDepositRepository
                .findByUserIdAndSubmittedFalseAndExpiresAtAfterOrderByCreatedAtDesc(userId, Instant.now());

        return pending.stream()
                .filter(p -> p.getChain().equalsIgnoreCase(chain))
                .filter(p -> p.getBaseAmount().compareTo(baseAmount) == 0)
                .findFirst();
    }

    public List<PendingDeposit> getActivePendingDeposits(UUID userId) {
        return pendingDepositRepository
                .findByUserIdAndSubmittedFalseAndExpiresAtAfterOrderByCreatedAtDesc(userId, Instant.now());
    }

    public Optional<PendingDeposit> getPendingDeposit(UUID id, UUID userId) {
        return pendingDepositRepository.findByIdAndUserId(id, userId);
    }

    @Transactional
    public void markAsSubmitted(UUID pendingDepositId, String transactionHash) {
        PendingDeposit pending = pendingDepositRepository.findById(pendingDepositId)
                .orElseThrow(() -> new RuntimeException("Pending deposit not found"));
        pending.setSubmitted(true);
        pending.setTransactionHash(transactionHash);
        pendingDepositRepository.save(pending);
        System.out.println("✅ Marked pending deposit as submitted: " + pendingDepositId);
    }

    @Transactional
    public void cancelPendingDeposit(UUID id, UUID userId) {
        PendingDeposit pending = pendingDepositRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Pending deposit not found"));
        pendingDepositRepository.delete(pending);
        System.out.println("🗑️ Cancelled pending deposit: " + id);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredPendingDeposits() {
        List<PendingDeposit> expired = pendingDepositRepository
                .findBySubmittedFalseAndExpiresAtBefore(Instant.now());
        if (!expired.isEmpty()) {
            pendingDepositRepository.deleteAll(expired);
            System.out.println("🧹 Cleaned up " + expired.size() + " expired pending deposits");
        }
    }

    private BigDecimal generateSecurityAmount() {
        int cents = new Random().nextInt(100) + 1;
        return new BigDecimal(cents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}