package com.fpsweeper.harvest.points.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class DepositRequest {

    @NotBlank(message = "Transaction hash is required")
    private String transactionHash;

    @NotBlank(message = "Chain is required")
    private String chain;

    @NotNull(message = "Exact amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal exactAmountUsd;

    // ✅ NEW: Optional pending deposit ID
    private UUID pendingDepositId;

    // Getters and Setters
    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public BigDecimal getExactAmountUsd() {
        return exactAmountUsd;
    }

    public void setExactAmountUsd(BigDecimal exactAmountUsd) {
        this.exactAmountUsd = exactAmountUsd;
    }

    public UUID getPendingDepositId() {
        return pendingDepositId;
    }

    public void setPendingDepositId(UUID pendingDepositId) {
        this.pendingDepositId = pendingDepositId;
    }
}