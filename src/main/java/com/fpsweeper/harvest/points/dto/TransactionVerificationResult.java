package com.fpsweeper.harvest.points.dto;

import java.math.BigDecimal;

public class TransactionVerificationResult {

    private boolean exists;
    private boolean verified;
    private String status;  // "PENDING", "SUCCESS", "FAILED"
    private Integer confirmations;
    private Integer requiredConfirmations;
    private Long blockNumber;
    private String fromAddress;
    private BigDecimal amount;
    private String failureReason;

    // Private constructor - use static factory methods
    private TransactionVerificationResult() {}

    // Getters
    public boolean isExists() {
        return exists;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getStatus() {
        return status;
    }

    public Integer getConfirmations() {
        return confirmations;
    }

    public Integer getRequiredConfirmations() {
        return requiredConfirmations;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    // Setters
    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setConfirmations(Integer confirmations) {
        this.confirmations = confirmations;
    }

    public void setRequiredConfirmations(Integer requiredConfirmations) {
        this.requiredConfirmations = requiredConfirmations;
    }

    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    // ✅ STATIC FACTORY METHODS

    /**
     * Transaction not found on blockchain
     */
    public static TransactionVerificationResult notFound(String reason) {
        TransactionVerificationResult result = new TransactionVerificationResult();
        result.exists = false;
        result.verified = false;
        result.status = "FAILED";
        result.failureReason = reason;
        return result;
    }

    /**
     * Transaction found but verification failed
     */
    public static TransactionVerificationResult failed(String reason) {
        TransactionVerificationResult result = new TransactionVerificationResult();
        result.exists = true;
        result.verified = false;
        result.status = "FAILED";
        result.failureReason = reason;
        return result;
    }

    /**
     * Transaction found but pending confirmations
     */
    public static TransactionVerificationResult pending(
            int confirmations,
            int requiredConfirmations,
            Long blockNumber,
            String fromAddress,
            BigDecimal amount
    ) {
        TransactionVerificationResult result = new TransactionVerificationResult();
        result.exists = true;
        result.verified = false;
        result.status = "PENDING";
        result.confirmations = confirmations;
        result.requiredConfirmations = requiredConfirmations;
        result.blockNumber = blockNumber;
        result.fromAddress = fromAddress;
        result.amount = amount;
        return result;
    }

    /**
     * Transaction fully verified and confirmed
     */
    public static TransactionVerificationResult success(
            int confirmations,
            Long blockNumber,
            String fromAddress,
            BigDecimal amount
    ) {
        TransactionVerificationResult result = new TransactionVerificationResult();
        result.exists = true;
        result.verified = true;
        result.status = "SUCCESS";
        result.confirmations = confirmations;
        result.blockNumber = blockNumber;
        result.fromAddress = fromAddress;
        result.amount = amount;
        return result;
    }

    @Override
    public String toString() {
        return "TransactionVerificationResult{" +
                "exists=" + exists +
                ", verified=" + verified +
                ", status='" + status + '\'' +
                ", confirmations=" + confirmations +
                ", requiredConfirmations=" + requiredConfirmations +
                ", blockNumber=" + blockNumber +
                ", fromAddress='" + fromAddress + '\'' +
                ", amount=" + amount +
                ", failureReason='" + failureReason + '\'' +
                '}';
    }
}