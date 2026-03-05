package com.fpsweeper.harvest.points.controller;

import com.fpsweeper.harvest.auth.exceptions.DepositException;
import com.fpsweeper.harvest.points.*;
import com.fpsweeper.harvest.points.dto.DepositRequest;
import com.fpsweeper.harvest.points.dto.DepositResponse;
import com.fpsweeper.harvest.user.Users;
import com.fpsweeper.harvest.wallet.SolanaWalletController;
import com.fpsweeper.harvest.wallet.SolanaWalletRepository;
import com.fpsweeper.harvest.wallet.SolanaWalletService;
import com.fpsweeper.harvest.wallet.UserWallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/deposits")
@CrossOrigin(origins = "*")
public class DepositController {

    @Autowired
    private DepositService depositService;

    @Autowired
    private ChainService chainService;

    @Autowired
    private SolanaWalletService walletService;

    @Autowired
    private SolanaWalletRepository walletRep;

    @Autowired
    private PendingDepositService pendingDepositService;

    /**
     * Submit a deposit
     * POST /api/deposits/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitDeposit(
            @AuthenticationPrincipal Users user,
            @Validated @RequestBody DepositRequest request
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            PointDeposit deposit = depositService.submitDeposit(
                    user.getId(),
                    request.getTransactionHash(),
                    request.getChain(),
                    request.getExactAmountUsd()
            );

            // ✅ NEW: Mark pending deposit as submitted (if exists)
            if (request.getPendingDepositId() != null) {
                try {
                    pendingDepositService.markAsSubmitted(
                            request.getPendingDepositId(),
                            request.getTransactionHash()
                    );
                } catch (Exception e) {
                    // Pending deposit might have expired or been deleted - that's okay
                    System.out.println("⚠️ Could not mark pending deposit as submitted: " + e.getMessage());
                }
            }

            DepositResponse response = new DepositResponse(deposit);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            System.err.println("Error submitting deposit: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get deposit instructions
     * GET /api/deposits/instructions/{chain}?amount=100
     */
    @GetMapping("/instructions/{chain}")
    public ResponseEntity<?> getDepositInstructions(
            @AuthenticationPrincipal Users user,
            @PathVariable String chain,
            @RequestParam BigDecimal amount
    ) {
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized"));
        }

        try {
            // ✅ Create pending deposit (this generates and saves the random amount)
            PendingDeposit pending = pendingDepositService.createPendingDeposit(
                    user.getId(),
                    chain,
                    amount
            );

            // ✅ Build instructions using data FROM pending deposit (not generating new amount!)
            Map<String, Object> instructions = new HashMap<>();
            instructions.put("chain", pending.getChain());
            instructions.put("depositAddress", pending.getPlatformWallet());
            instructions.put("baseAmount", pending.getBaseAmount());
            instructions.put("securityAmount", pending.getSecurityAmount());  // ✅ From pending deposit
            instructions.put("exactAmount", pending.getExactAmount());  // ✅ From pending deposit
            instructions.put("amount", pending.getBaseAmount());  // Backward compatibility
            instructions.put("token", "USDT");
            instructions.put("tokenAddress", pending.getTokenAddress());
            instructions.put("pointsToReceive", pending.getPointsToReceive());

            // Check for linked wallet
            Optional<UserWallet> linkedWallet = walletRep.findByUserIdAndChain(
                    user.getId(),
                    chain.toUpperCase()
            );
            instructions.put("hasLinkedWallet", linkedWallet.isPresent());
            if (linkedWallet.isPresent()) {
                instructions.put("linkedWalletAddress", linkedWallet.get().getWalletAddress());
            }

            // ✅ Instructions with exact amount from pending deposit
            Map<String, String> steps = new HashMap<>();
            steps.put("step1", "Send EXACTLY " + pending.getExactAmount() + " USDT to the address above");
            steps.put("step2", "IMPORTANT: You must send " + pending.getExactAmount() +
                    " (includes $" + pending.getSecurityAmount() + " security amount)");
            steps.put("step3", "Copy the transaction hash after sending");
            steps.put("step4", "Submit the transaction hash below");
            steps.put("step5", "Your " + pending.getPointsToReceive() + " points will be credited after confirmation");
            instructions.put("instructions", steps);

            // ✅ Include pending deposit ID and expiry
            instructions.put("pendingDepositId", pending.getId());
            instructions.put("expiresAt", pending.getExpiresAt());
            instructions.put("timeRemainingSeconds", pending.getTimeRemainingSeconds());

            System.out.println("📋 Returning deposit instructions from pending deposit:");
            System.out.println("   Pending ID: " + pending.getId());
            System.out.println("   Exact Amount: $" + pending.getExactAmount());
            System.out.println("   Security Amount: $" + pending.getSecurityAmount());

            return ResponseEntity.ok(instructions);

        } catch (RuntimeException e) {
            System.err.println("Error getting deposit instructions: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get deposit history
     * GET /api/deposits/history?page=0&size=20
     */
    @GetMapping("/history")
    public ResponseEntity<Page<DepositResponse>> getDepositHistory(
            @AuthenticationPrincipal Users user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Page<PointDeposit> deposits = depositService.getDepositHistory(
                user.getId(),
                page,
                size
        );

        Page<DepositResponse> response = deposits.map(this::mapToResponse);

        return ResponseEntity.ok(response);
    }

    /**
     * Get deposit status by transaction hash
     * GET /api/deposits/status/{transactionHash}
     */
    @GetMapping("/status/{transactionHash}")
    public ResponseEntity<?> getDepositStatus(
            @AuthenticationPrincipal Users user,
            @PathVariable String transactionHash
    ) {
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Unauthorized"));
        }

        Optional<PointDeposit> deposit = depositService.getDepositByHash(transactionHash);

        if (deposit.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Deposit not found"));
        }

        // Verify this deposit belongs to the current user
        if (!deposit.get().getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
        }

        DepositResponse response = mapToResponse(deposit.get());

        return ResponseEntity.ok(response);
    }

    /**
     * Generate memo for user
     */
    private String generateMemo(java.util.UUID userId) {
        // Simple memo format: USER_{first 8 chars of UUID}
        return "USER_" + userId.toString().substring(0, 8).toUpperCase();
    }

    /**
     * Map PointDeposit to DepositResponse
     */
    private DepositResponse mapToResponse(PointDeposit deposit) {
        DepositResponse response = new DepositResponse();
        response.setId(deposit.getId());
        response.setTransactionHash(deposit.getTransactionHash());
        response.setChain(deposit.getChain());
        response.setMemo(deposit.getMemo()); // ✅ Include memo
        response.setAmountUsd(deposit.getAmountUsd());
        response.setPointsIssued(deposit.getPointsIssued());
        response.setStatus(deposit.getStatus());
        response.setFailureReason(deposit.getFailureReason());
        response.setSubmittedAt(deposit.getSubmittedAt());
        response.setConfirmedAt(deposit.getConfirmedAt());
        return response;
    }
}