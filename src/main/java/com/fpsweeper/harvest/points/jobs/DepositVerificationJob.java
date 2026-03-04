package com.fpsweeper.harvest.points.jobs;

import com.fpsweeper.harvest.points.DepositService;
import com.fpsweeper.harvest.points.PointDeposit;
import com.fpsweeper.harvest.points.blockchain.BlockchainVerificationService;
import com.fpsweeper.harvest.points.dto.TransactionVerificationResult;
import com.fpsweeper.harvest.wallet.SolanaWalletRepository;
import com.fpsweeper.harvest.wallet.UserWallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepositVerificationJob {

    @Autowired
    private DepositService depositService;

    @Autowired
    private BlockchainVerificationService blockchainVerificationService;

    @Value("${blockchain.verification.enabled:true}")
    private boolean verificationEnabled;

    @Autowired
    private SolanaWalletRepository walletRepository;

    /**
     * Verify pending deposits every 30 seconds
     */
    @Scheduled(fixedRate = 30000, initialDelay = 10000)
    public void verifyPendingDeposits() {
        if (!verificationEnabled) {
            System.out.println("⏸️ Deposit verification is disabled");
            return;
        }

        try {
            List<PointDeposit> pendingDeposits = depositService.getPendingDeposits();

            System.out.println("🔍 Checking pending deposits... Found: " + pendingDeposits.size());

            if (pendingDeposits.isEmpty()) {
                return;
            }

            for (PointDeposit deposit : pendingDeposits) {
                verifyDeposit(deposit);
            }

        } catch (Exception e) {
            System.err.println("❌ Error in deposit verification job: " + e.getMessage());
            e.printStackTrace();
        }
    }



    private void verifyDeposit(PointDeposit deposit) {
        try {
            System.out.println("🔍 Verifying deposit: " + deposit.getTransactionHash() +
                    " on " + deposit.getChain());

            // ✅ Verify with EXACT amount (no tolerance!)
            TransactionVerificationResult result = blockchainVerificationService.verifyTransaction(
                    deposit.getChain(),
                    deposit.getTransactionHash(),
                    deposit.getExactAmountUsd()  // ✅ Use exact amount
            );

            if (!result.isExists()) {
                System.out.println("⚠️ Transaction not found: " + deposit.getTransactionHash());
                depositService.failDeposit(
                        deposit.getId(),
                        result.getFailureReason()
                );
                return;
            }

            if (!result.isVerified()) {
                if ("PENDING".equals(result.getStatus())) {
                    System.out.println("⏳ Waiting for confirmations: " +
                            result.getConfirmations() + " confirmations");

                    depositService.updateDepositVerification(
                            deposit.getId(),
                            result.getConfirmations(),
                            result.getBlockNumber()
                    );
                } else {
                    System.out.println("❌ Verification failed: " + result.getFailureReason());
                    depositService.failDeposit(
                            deposit.getId(),
                            result.getFailureReason()
                    );
                }
                return;
            }

            // ✅ Sender verification (optional now)
            String senderAddress = result.getFromAddress();
            Optional<UserWallet> linkedWallet = walletRepository.findByWalletAddress(senderAddress);

            if (linkedWallet.isPresent()) {
                if (!linkedWallet.get().getUserId().equals(deposit.getUserId())) {
                    depositService.failDeposit(
                            deposit.getId(),
                            "Transaction sent from wallet linked to different user"
                    );
                    return;
                }
                System.out.println("✅ Sender wallet verified: linked to user " + deposit.getUserId());
            } else {
                System.out.println("ℹ️ Transaction from non-linked wallet (allowed with exact amount)");
            }

            // Verified!
            System.out.println("✅ Deposit verified: " + deposit.getTransactionHash());
            System.out.println("   From: " + result.getFromAddress());
            System.out.println("   Exact Amount: $" + result.getAmount());
            System.out.println("   Expected: $" + deposit.getExactAmountUsd());
            System.out.println("   Confirmations: " + result.getConfirmations());

            depositService.confirmDeposit(
                    deposit.getId(),
                    result.getFromAddress(),
                    result.getBlockNumber(),
                    result.getConfirmations()
            );

        } catch (Exception e) {
            System.err.println("❌ Error verifying deposit " + deposit.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Check verified deposits awaiting final confirmation
     */
    @Scheduled(fixedRate = 60000, initialDelay = 20000)
    public void checkVerifiedDeposits() {
        if (!verificationEnabled) {
            return;
        }

        try {
            List<PointDeposit> verifiedDeposits = depositService.getVerifiedDeposits();

            if (verifiedDeposits.isEmpty()) {
                return;
            }

            System.out.println("🔍 Checking " + verifiedDeposits.size() +
                    " verified deposits for final confirmation...");

            for (PointDeposit deposit : verifiedDeposits) {
                verifyDeposit(deposit);
            }

        } catch (Exception e) {
            System.err.println("❌ Error checking verified deposits: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Log system status every 5 minutes
     */
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void logStatus() {
        try {
            List<PointDeposit> pending = depositService.getPendingDeposits();
            List<PointDeposit> verified = depositService.getVerifiedDeposits();

            System.out.println("📊 Deposit Verification Status:");
            System.out.println("   Pending: " + pending.size());
            System.out.println("   Verified (awaiting confirmation): " + verified.size());

        } catch (Exception e) {
            // Silent fail for status logging
        }
    }
}