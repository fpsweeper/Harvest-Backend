package com.fpsweeper.harvest.points;

import com.fpsweeper.harvest.auth.exceptions.DepositException;
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

    @Autowired
    private PointDepositRepository depositRepository;

    @Autowired
    private SupportedChainRepository chainRepository;

    @Autowired
    private PointsService pointsService;

    @Autowired
    private SolanaWalletRepository walletRepository;

    @Value("${points.conversion.rate:0.5}")
    private BigDecimal conversionRate;

    private static final BigDecimal CONVERSION_RATE = new BigDecimal("0.5");

    /**
     * Get current conversion rate (USD to Points)
     */
    public BigDecimal getConversionRate() {
        // In future, you could fetch this from database or configuration
        // For now, hardcoded: $1 = 0.5 points
        return CONVERSION_RATE;
    }

    /**
     * Get list of supported chains
     */
    public List<SupportedChain> getSupportedChains() {
        return chainRepository.findByIsActive(true);
    }

    /**
     * Get specific chain
     */
    public Optional<SupportedChain> getChain(String chainName) {
        return chainRepository.findByChainNameAndIsActive(chainName.toUpperCase(), true);
    }

    /**
     * Calculate points from USD amount
     */
    public BigDecimal calculatePoints(BigDecimal amountUsd) {
        return amountUsd.multiply(getConversionRate())
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Submit a new deposit
     */
    @Transactional
    public PointDeposit submitDeposit(
            UUID userId,
            String transactionHash,
            String chain,
            BigDecimal amountUsd,
            String memo // ✅ NEW: memo parameter
    ) {
        // Validate chain is supported
        SupportedChain supportedChain = chainRepository
                .findByChainNameAndIsActive(chain.toUpperCase(), true)
                .orElseThrow(() -> new DepositException("Chain not supported: " + chain));

        // Check minimum deposit
        if (amountUsd.compareTo(supportedChain.getMinDepositUsd()) < 0) {
            throw new DepositException(
                    "Minimum deposit is $" + supportedChain.getMinDepositUsd()
            );
        }

        // Check if transaction hash already exists
        if (depositRepository.existsByTransactionHash(transactionHash)) {
            throw new DepositException("Transaction already submitted");
        }

        // Calculate points
        BigDecimal points = calculatePoints(amountUsd);

        PointDeposit deposit = new PointDeposit();
        deposit.setUserId(userId);
        deposit.setTransactionHash(transactionHash);
        deposit.setChain(chain.toUpperCase());
        deposit.setMemo(memo); // Optional now
        deposit.setAmountUsd(amountUsd);
        deposit.setPointsIssued(points);
        deposit.setConversionRate(conversionRate);
        deposit.setStatus("PENDING");

        PointDeposit saved = depositRepository.save(deposit);

        System.out.println("📝 Deposit submitted: " + transactionHash +
                " | User: " + userId +
                " | Chain: " + chain +
                " | Amount: $" + amountUsd +
                " | Points: " + points +
                (memo != null ? " | Memo: " + memo : " | (no memo - wallet linked)"));

        return saved;
    }

    /**
     * Confirm a deposit and credit points
     */
    @Transactional
    public void confirmDeposit(
            UUID depositId,
            String fromWallet,
            Long blockNumber,
            Integer confirmations
    ) {
        PointDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));

        if ("CONFIRMED".equals(deposit.getStatus())) {
            System.out.println("⚠️ Deposit already confirmed: " + depositId);
            return;
        }

        // Update deposit status
        deposit.setStatus("CONFIRMED");
        deposit.setFromWallet(fromWallet);
        deposit.setBlockNumber(blockNumber);
        deposit.setConfirmations(confirmations);
        deposit.setConfirmedAt(Instant.now());
        depositRepository.save(deposit);

        // Credit points to user
        pointsService.addPoints(
                deposit.getUserId(),
                deposit.getPointsIssued(),
                "Deposit confirmed: " + deposit.getTransactionHash(),
                deposit.getId()
        );

        System.out.println("✅ Deposit confirmed: " + deposit.getTransactionHash() +
                " | User: " + deposit.getUserId() +
                " | Points added: " + deposit.getPointsIssued());
    }

    /**
     * Mark deposit as failed
     */
    @Transactional
    public void failDeposit(UUID depositId, String reason) {
        PointDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));

        deposit.setStatus("FAILED");
        deposit.setFailureReason(reason);
        depositRepository.save(deposit);

        System.out.println("❌ Deposit failed: " + deposit.getTransactionHash() +
                " | Reason: " + reason);
    }

    /**
     * Update deposit verification progress
     */
    @Transactional
    public void updateDepositVerification(
            UUID depositId,
            Integer confirmations,
            Long blockNumber
    ) {
        PointDeposit deposit = depositRepository.findById(depositId)
                .orElseThrow(() -> new RuntimeException("Deposit not found"));

        deposit.setStatus("VERIFIED");
        deposit.setConfirmations(confirmations);
        deposit.setBlockNumber(blockNumber);
        deposit.setVerifiedAt(Instant.now());
        depositRepository.save(deposit);
    }

    /**
     * Get deposit history for user
     */
    public Page<PointDeposit> getDepositHistory(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        return depositRepository.findByUserId(userId, pageable);
    }

    /**
     * Get deposit by transaction hash
     */
    public Optional<PointDeposit> getDepositByHash(String transactionHash) {
        return depositRepository.findByTransactionHash(transactionHash);
    }

    /**
     * Get pending deposits for background job
     */
    public List<PointDeposit> getPendingDeposits() {
        return depositRepository.findByStatusOrderBySubmittedAtAsc("PENDING");
    }

    /**
     * Get verified deposits awaiting confirmation
     */
    public List<PointDeposit> getVerifiedDeposits() {
        return depositRepository.findByStatusOrderBySubmittedAtAsc("VERIFIED");
    }

    /**
     * Generate random security amount between 0.01 and 1.00
     */
    private BigDecimal generateSecurityAmount() {
        Random random = new Random();
        // Generate random cents between 1 and 100
        int cents = random.nextInt(100) + 1; // 1 to 100 cents
        return new BigDecimal(cents).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Get deposit instructions with exact amount including security digits
     */
    public Map<String, Object> getDepositInstructions(UUID userId, String chain, BigDecimal baseAmount) {
        // Get chain config
        SupportedChain supportedChain = chainRepository.findByChainName(chain.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Chain not supported: " + chain));

        // Validate minimum deposit
        if (baseAmount.compareTo(supportedChain.getMinDepositUsd()) < 0) {
            throw new RuntimeException("Minimum deposit is $" + supportedChain.getMinDepositUsd());
        }

        // ✅ Generate random security amount
        BigDecimal securityAmount = generateSecurityAmount();
        BigDecimal exactAmount = baseAmount.add(securityAmount);

        // Calculate points based on BASE amount (not including security digits)
        BigDecimal conversionRate = getConversionRate();
        BigDecimal points = baseAmount.multiply(conversionRate)
                .setScale(2, RoundingMode.HALF_UP);

        // Get user's linked wallet (optional)
        Optional<UserWallet> linkedWallet = walletRepository.findByUserIdAndChain(
                userId,
                chain.toUpperCase()
        );

        Map<String, Object> instructions = new HashMap<>();
        instructions.put("chain", chain.toUpperCase());
        instructions.put("depositAddress", supportedChain.getPlatformWalletAddress());
        instructions.put("baseAmount", baseAmount);  // ✅ Original amount
        instructions.put("securityAmount", securityAmount);  // ✅ Random amount
        instructions.put("exactAmount", exactAmount);  // ✅ Total amount to send
        instructions.put("token", "USDC");
        instructions.put("tokenAddress", supportedChain.getUsdcTokenAddress());
        instructions.put("pointsToReceive", points);
        instructions.put("hasLinkedWallet", linkedWallet.isPresent());

        if (linkedWallet.isPresent()) {
            instructions.put("linkedWalletAddress", linkedWallet.get().getWalletAddress());
        }

        // ✅ Updated instructions
        Map<String, String> steps = new HashMap<>();
        steps.put("step1", "Send exactly " + exactAmount + " USDC to the address above");
        steps.put("step2", "IMPORTANT: Send exactly " + exactAmount + " (including " + securityAmount + " security amount)");
        steps.put("step3", "Copy the transaction hash after sending");
        steps.put("step4", "Submit the transaction hash below");
        steps.put("step5", "Your " + points + " points will be credited after confirmation");

        instructions.put("instructions", steps);

        return instructions;
    }

    /**
     * Submit a deposit with exact amount verification
     */
    @Transactional
    public PointDeposit submitDeposit(
            UUID userId,
            String transactionHash,
            String chain,
            BigDecimal exactAmountUsd  // ✅ User submits the exact amount they sent
    ) {
        // Check for duplicate transaction
        if (depositRepository.existsByTransactionHash(transactionHash)) {
            throw new RuntimeException("Transaction already submitted");
        }

        // Validate chain
        SupportedChain supportedChain = chainRepository.findByChainName(chain.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Chain not supported: " + chain));

        // Extract base amount (remove cents to get package amount)
        // Example: $50.37 → $50.00
        BigDecimal baseAmount = exactAmountUsd.setScale(0, RoundingMode.DOWN);

        // Validate minimum (base amount)
        if (baseAmount.compareTo(supportedChain.getMinDepositUsd()) < 0) {
            throw new RuntimeException("Minimum deposit is $" + supportedChain.getMinDepositUsd());
        }

        // Calculate points based on BASE amount only
        BigDecimal conversionRate = getConversionRate();
        BigDecimal points = baseAmount.multiply(conversionRate)
                .setScale(2, RoundingMode.HALF_UP);

        // Create deposit record
        PointDeposit deposit = new PointDeposit();
        deposit.setUserId(userId);
        deposit.setTransactionHash(transactionHash);
        deposit.setChain(chain.toUpperCase());
        deposit.setAmountUsd(baseAmount);  // Base amount (e.g., $50)
        deposit.setExactAmountUsd(exactAmountUsd);  // ✅ Exact amount (e.g., $50.37)
        deposit.setPointsIssued(points);
        deposit.setConversionRate(conversionRate);
        deposit.setStatus("PENDING");

        PointDeposit saved = depositRepository.save(deposit);

        System.out.println("📝 Deposit submitted: " + transactionHash +
                " | User: " + userId +
                " | Chain: " + chain +
                " | Base: $" + baseAmount +
                " | Exact: $" + exactAmountUsd +  // ✅ Log exact amount
                " | Points: " + points);

        return saved;
    }
}