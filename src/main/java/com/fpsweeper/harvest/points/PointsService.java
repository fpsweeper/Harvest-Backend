package com.fpsweeper.harvest.points;

import com.fpsweeper.harvest.auth.exceptions.InsufficientPointsException;
import com.fpsweeper.harvest.trading.TradingBot;
import com.fpsweeper.harvest.trading.TradingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PointsService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PointsService.class);

    @Autowired
    private UserPointsRepository userPointsRepository;

    @Autowired
    private PointTransactionRepository transactionRepository;

    /**
     * Get user's current point balance
     */
    public BigDecimal getBalance(UUID userId) {
        return userPointsRepository.findByUserId(userId)
                .map(UserPoints::getPoints)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Get or create user points record
     */
    private UserPoints getOrCreateUserPoints(UUID userId) {
        return userPointsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPoints newPoints = new UserPoints(userId);
                    return userPointsRepository.save(newPoints);
                });
    }

    /**
     * Add points to user balance (e.g., from deposit)
     * Uses pessimistic locking to prevent race conditions
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addPoints(UUID userId, BigDecimal amount, String description, UUID depositId) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Get user points with lock
        UserPoints userPoints = userPointsRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    UserPoints newPoints = new UserPoints(userId);
                    return userPointsRepository.save(newPoints);
                });

        BigDecimal balanceBefore = userPoints.getPoints();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        // Update balance
        userPoints.setPoints(balanceAfter);
        userPoints.setLastUpdated(Instant.now());
        userPointsRepository.save(userPoints);

        // Create transaction record
        PointTransaction transaction = new PointTransaction();
        transaction.setUserId(userId);
        transaction.setType("DEPOSIT");
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setDepositId(depositId);
        transactionRepository.save(transaction);

        System.out.println("✅ Added " + amount + " points to user " + userId +
                ". New balance: " + balanceAfter);
    }

    /**
     * Deduct points from user balance (e.g., for bot usage)
     * Uses pessimistic locking to prevent race conditions
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deductPoints(UUID userId, BigDecimal amount, String description, UUID botExecutionId)
            throws InsufficientPointsException {

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Get user points with lock
        UserPoints userPoints = userPointsRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new InsufficientPointsException("User has no points"));

        BigDecimal balanceBefore = userPoints.getPoints();

        // Check sufficient balance
        if (balanceBefore.compareTo(amount) < 0) {
            throw new InsufficientPointsException(
                    "Insufficient points. Required: " + amount + ", Available: " + balanceBefore
            );
        }

        BigDecimal balanceAfter = balanceBefore.subtract(amount);

        // Update balance
        userPoints.setPoints(balanceAfter);
        userPoints.setLastUpdated(Instant.now());
        userPointsRepository.save(userPoints);

        // Create transaction record
        PointTransaction transaction = new PointTransaction();
        transaction.setUserId(userId);
        transaction.setType("BOT_USAGE");
        transaction.setAmount(amount.negate()); // Negative for deduction
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription(description);
        transaction.setBotExecutionId(botExecutionId);
        transactionRepository.save(transaction);

        System.out.println("✅ Deducted " + amount + " points from user " + userId +
                ". New balance: " + balanceAfter);
    }

    /**
     * Deduct points for a bot execution — mode-aware.
     *
     * SIMULATION bots: always a no-op. They run on free virtual credit and must
     * never touch the user's real deposited points balance under any circumstances.
     *
     * LIVE bots (future): deduct from real points balance as normal.
     *
     * This is the ONLY method that should be called from the execution pipeline.
     * Never call deductPoints() directly from bot execution code.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deductPointsForBot(TradingBot bot) throws InsufficientPointsException {
        // Simulation bots are free — skip entirely
        if (bot.getTradingMode() == TradingMode.SIMULATION || bot.isVirtualCredit()) {
            log.debug("⏭️ Skipping point deduction for simulation bot: {}", bot.getName());
            return;
        }

        // LIVE bots: deduct real points
        BigDecimal cost = bot.getPointsPerDay() != null ? bot.getPointsPerDay() : BigDecimal.ONE;
        String desc = "Bot execution: " + bot.getName() + " (" + bot.getTradingMode() + ")";
        deductPoints(bot.getUserId(), cost, desc, bot.getId());
        log.info("💰 Deducted {} points from user {} for LIVE bot {}",
                cost, bot.getUserId(), bot.getName());
    }

    /**
     * Check if user has enough points — simulation bots always return true.
     */
    public boolean hasEnoughPointsForBot(TradingBot bot) {
        if (bot.getTradingMode() == TradingMode.SIMULATION || bot.isVirtualCredit()) {
            return true; // Simulation is always free
        }
        BigDecimal cost = bot.getPointsPerDay() != null ? bot.getPointsPerDay() : BigDecimal.ONE;
        return hasEnoughPoints(bot.getUserId(), cost);
    }

    /**
     * Check if user has enough points
     */
    public boolean hasEnoughPoints(UUID userId, BigDecimal requiredAmount) {
        BigDecimal balance = getBalance(userId);
        return balance.compareTo(requiredAmount) >= 0;
    }

    /**
     * Get transaction history for user
     */
    public Page<PointTransaction> getTransactionHistory(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get all transactions for user (unpaginated)
     */
    public List<PointTransaction> getAllTransactions(UUID userId) {
        return transactionRepository.findByUserId(userId);
    }

    /**
     * Admin: Manually adjust points (for support/refunds)
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void adminAdjustPoints(UUID userId, BigDecimal amount, String reason) {
        UserPoints userPoints = userPointsRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> {
                    UserPoints newPoints = new UserPoints(userId);
                    return userPointsRepository.save(newPoints);
                });

        BigDecimal balanceBefore = userPoints.getPoints();
        BigDecimal balanceAfter = balanceBefore.add(amount);

        if (balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Adjustment would result in negative balance");
        }

        userPoints.setPoints(balanceAfter);
        userPoints.setLastUpdated(Instant.now());
        userPointsRepository.save(userPoints);

        // Create transaction record
        PointTransaction transaction = new PointTransaction();
        transaction.setUserId(userId);
        transaction.setType("ADMIN_ADJUSTMENT");
        transaction.setAmount(amount);
        transaction.setBalanceBefore(balanceBefore);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setDescription("Admin adjustment: " + reason);
        transactionRepository.save(transaction);

        System.out.println("✅ Admin adjusted points for user " + userId +
                " by " + amount + ". Reason: " + reason);
    }
}