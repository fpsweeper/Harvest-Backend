package com.fpsweeper.harvest.points;

import com.fpsweeper.harvest.auth.exceptions.DepositException;
import com.fpsweeper.harvest.notification.NotificationService;
import com.fpsweeper.harvest.wallet.SolanaWalletRepository;
import com.fpsweeper.harvest.wallet.UserWallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
public class DepositService {

    @Autowired private PointDepositRepository depositRepository;
    @Autowired private SupportedChainRepository chainRepository;
    @Autowired private PointsService pointsService;
    @Autowired private SolanaWalletRepository walletRepository;
    @Autowired private NotificationService notificationService;

    // ✅ Inject the packages repository so we can look up points by price
    @Autowired private PointsPackageRepository packagesRepository;

    @Value("${points.conversion.rate:0.5}")
    private BigDecimal conversionRate;

    /**
     * Look up points for a given USD base amount from the points_packages table.
     * Falls back to flat conversion rate if no matching package is found.
     *
     * Examples with current packages:
     *   $10  → 30 pts
     *   $50  → 200 pts
     *   $200 → 900 pts
     *   $400 → 2000 pts
     */
    public BigDecimal calculatePoints(BigDecimal baseAmountUsd) {
        return packagesRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .filter(p -> p.getPriceUsd().compareTo(baseAmountUsd) == 0)
                .map(PointsPackage::getPoints)
                .findFirst()
                .orElseGet(() -> {
                    // Fallback: flat rate (should never happen for valid packages)
                    System.out.println("⚠️ No package found for $" + baseAmountUsd + " — using flat conversion rate");
                    return baseAmountUsd.multiply(conversionRate).setScale(2, RoundingMode.HALF_UP);
                });
    }

    public BigDecimal getConversionRate() { return conversionRate; }

    public List<SupportedChain> getSupportedChains() {
        return chainRepository.findByIsActive(true);
    }

    public Optional<SupportedChain> getChain(String chainName) {
        return chainRepository.findByChainNameAndIsActive(chainName.toUpperCase(), true);
    }

    // ── Submit deposit (with memo) ─────────────────────────────────────────

    @Transactional
    public PointDeposit submitDeposit(
            UUID userId,
            String transactionHash,
            String chain,
            BigDecimal amountUsd,
            String memo
    ) {
        SupportedChain supportedChain = chainRepository
                .findByChainNameAndIsActive(chain.toUpperCase(), true)
                .orElseThrow(() -> new DepositException("Chain not supported: " + chain));

        if (amountUsd.compareTo(supportedChain.getMinDepositUsd()) < 0)
            throw new DepositException("Minimum deposit is $" + supportedChain.getMinDepositUsd());

        if (depositRepository.existsByTransactionHash(transactionHash))
            throw new DepositException("Transaction already submitted");

        // ✅ Use package-based points lookup
        BigDecimal points = calculatePoints(amountUsd);

        PointDeposit deposit = new PointDeposit();
        deposit.setUserId(userId);
        deposit.setTransactionHash(transactionHash);
        deposit.setChain(chain.toUpperCase());
        deposit.setMemo(memo);
        deposit.setAmountUsd(amountUsd);
        deposit.setPointsIssued(points);
        deposit.setConversionRate(conversionRate);
        deposit.setStatus("PENDING");

        PointDeposit saved = depositRepository.save(deposit);

        System.out.println("📝 Deposit submitted: " + transactionHash +
                " | User: " + userId + " | Chain: " + chain +
                " | Amount: $" + amountUsd + " | Points: " + points +
                (memo != null ? " | Memo: " + memo : ""));

        return saved;
    }

    // ── Submit deposit (exact amount — main flow) ──────────────────────────

    @Transactional
    public PointDeposit submitDeposit(
            UUID userId,
            String transactionHash,
            String chain,
            BigDecimal exactAmountUsd
    ) {
        if (depositRepository.existsByTransactionHash(transactionHash))
            throw new RuntimeException("Transaction already submitted");

        SupportedChain supportedChain = chainRepository.findByChainName(chain.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Chain not supported: " + chain));

        // Strip security cents: $200.24 → $200
        BigDecimal baseAmount = exactAmountUsd.setScale(0, RoundingMode.DOWN);

        if (baseAmount.compareTo(supportedChain.getMinDepositUsd()) < 0)
            throw new RuntimeException("Minimum deposit is $" + supportedChain.getMinDepositUsd());

        // ✅ Look up points from package — NOT flat rate
        BigDecimal points = calculatePoints(baseAmount);

        PointDeposit deposit = new PointDeposit();
        deposit.setUserId(userId);
        deposit.setTransactionHash(transactionHash);
        deposit.setChain(chain.toUpperCase());
        deposit.setAmountUsd(baseAmount);
        deposit.setExactAmountUsd(exactAmountUsd);
        deposit.setPointsIssued(points);
        deposit.setConversionRate(conversionRate);
        deposit.setStatus("PENDING");

        PointDeposit saved = depositRepository.save(deposit);

        System.out.println("📝 Deposit submitted: " + transactionHash +
                " | User: " + userId + " | Chain: " + chain +
                " | Base: $" + baseAmount + " | Exact: $" + exactAmountUsd +
                " | Points: " + points);

        return saved;
    }

    // ── Confirm deposit ────────────────────────────────────────────────────

    @Transactional
    public void confirmDeposit(UUID depositId, String fromWallet, Long blockNumber, Integer confirmations) {
        PointDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));

        if ("CONFIRMED".equals(deposit.getStatus())) {
            System.out.println("⚠️ Deposit already confirmed: " + depositId);
            return;
        }

        deposit.setStatus("CONFIRMED");
        deposit.setFromWallet(fromWallet);
        deposit.setBlockNumber(blockNumber);
        deposit.setConfirmations(confirmations);
        deposit.setConfirmedAt(Instant.now());
        depositRepository.save(deposit);

        pointsService.addPoints(
                deposit.getUserId(),
                deposit.getPointsIssued(),
                "Deposit confirmed: " + deposit.getTransactionHash(),
                deposit.getId()
        );

        System.out.println("✅ Deposit confirmed: " + deposit.getTransactionHash() +
                " | User: " + deposit.getUserId() +
                " | Points added: " + deposit.getPointsIssued());

        notificationService.notifyDepositSuccess(deposit.getUserId(), deposit.getPointsIssued());
    }

    // ── Fail / verify ──────────────────────────────────────────────────────

    @Transactional
    public void failDeposit(UUID depositId, String reason) {
        PointDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));
        deposit.setStatus("FAILED");
        deposit.setFailureReason(reason);
        depositRepository.save(deposit);
        System.out.println("❌ Deposit failed: " + deposit.getTransactionHash() + " | Reason: " + reason);
    }

    @Transactional
    public void updateDepositVerification(UUID depositId, Integer confirmations, Long blockNumber) {
        PointDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));
        deposit.setStatus("VERIFIED");
        deposit.setConfirmations(confirmations);
        deposit.setBlockNumber(blockNumber);
        deposit.setVerifiedAt(Instant.now());
        depositRepository.save(deposit);
    }

    // ── Queries ────────────────────────────────────────────────────────────

    public Page<PointDeposit> getDepositHistory(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        return depositRepository.findByUserId(userId, pageable);
    }

    public Optional<PointDeposit> getDepositByHash(String transactionHash) {
        return depositRepository.findByTransactionHash(transactionHash);
    }

    public List<PointDeposit> getPendingDeposits() {
        return depositRepository.findByStatusOrderBySubmittedAtAsc("PENDING");
    }

    public List<PointDeposit> getVerifiedDeposits() {
        return depositRepository.findByStatusOrderBySubmittedAtAsc("VERIFIED");
    }

    // ── Deposit instructions (legacy / unused by main flow) ───────────────

    private BigDecimal generateSecurityAmount() {
        int cents = new Random().nextInt(100) + 1;
        return new BigDecimal(cents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    public Map<String, Object> getDepositInstructions(UUID userId, String chain, BigDecimal baseAmount) {
        SupportedChain supportedChain = chainRepository.findByChainName(chain.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Chain not supported: " + chain));

        if (baseAmount.compareTo(supportedChain.getMinDepositUsd()) < 0)
            throw new RuntimeException("Minimum deposit is $" + supportedChain.getMinDepositUsd());

        BigDecimal securityAmount = generateSecurityAmount();
        BigDecimal exactAmount    = baseAmount.add(securityAmount);

        // ✅ Look up points from package
        BigDecimal points = calculatePoints(baseAmount);

        Optional<UserWallet> linkedWallet = walletRepository.findByUserIdAndChain(userId, chain.toUpperCase());

        Map<String, Object> instructions = new HashMap<>();
        instructions.put("chain", chain.toUpperCase());
        instructions.put("depositAddress", supportedChain.getPlatformWalletAddress());
        instructions.put("baseAmount", baseAmount);
        instructions.put("securityAmount", securityAmount);
        instructions.put("exactAmount", exactAmount);
        instructions.put("token", "USDC");
        instructions.put("tokenAddress", supportedChain.getUsdcTokenAddress());
        instructions.put("pointsToReceive", points);
        instructions.put("hasLinkedWallet", linkedWallet.isPresent());
        linkedWallet.ifPresent(w -> instructions.put("linkedWalletAddress", w.getWalletAddress()));

        Map<String, String> steps = new HashMap<>();
        steps.put("step1", "Send exactly " + exactAmount + " USDC to the address above");
        steps.put("step2", "IMPORTANT: Send exactly " + exactAmount + " (including " + securityAmount + " security amount)");
        steps.put("step3", "Copy the transaction hash after sending");
        steps.put("step4", "Submit the transaction hash below");
        steps.put("step5", "Your " + points + " points will be credited after confirmation");
        instructions.put("instructions", steps);

        return instructions;
    }
}