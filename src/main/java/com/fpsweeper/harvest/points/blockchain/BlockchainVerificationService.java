package com.fpsweeper.harvest.points.blockchain;

import com.fpsweeper.harvest.points.dto.TransactionVerificationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class BlockchainVerificationService {

    @Autowired
    private SolanaVerificationService solanaVerificationService;

    @Autowired(required = false)
    private ArbitrumVerificationService arbitrumVerificationService;

    @Autowired
    private BEP20VerificationService bep20VerificationService;

    @Autowired
    private TRC20VerificationService trc20VerificationService;

    /**
     * Verify transaction with recipient check
     */
    public TransactionVerificationResult verifyTransaction(
            String chain,
            String transactionHash,
            BigDecimal expectedAmount,
            String expectedRecipient
    ) {
        return switch (chain.toUpperCase()) {
            case "SOLANA" -> solanaVerificationService.verifyTransaction(
                    transactionHash,
                    expectedAmount,
                    expectedRecipient
            );
            case "ARBITRUM" -> arbitrumVerificationService != null
                    ? arbitrumVerificationService.verifyTransaction(
                    transactionHash,
                    expectedAmount,
                    expectedRecipient
            )
                    : TransactionVerificationResult.failed("Arbitrum not supported");
            case "BEP20", "BSC" -> bep20VerificationService.verifyTransaction(
                    transactionHash,
                    expectedAmount,
                    expectedRecipient
            );
            case "TRC20", "TRON" -> trc20VerificationService.verifyTransaction(
                    transactionHash,
                    expectedAmount,
                    expectedRecipient
            );
            default -> TransactionVerificationResult.failed("Unsupported chain: " + chain);
        };
    }

    /**
     * Verify transaction without recipient check (backward compatible)
     */
    public TransactionVerificationResult verifyTransaction(
            String chain,
            String transactionHash,
            BigDecimal expectedAmount
    ) {
        return verifyTransaction(chain, transactionHash, expectedAmount, null);
    }
}