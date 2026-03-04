package com.fpsweeper.harvest.points.dto;

import com.fpsweeper.harvest.points.PointDeposit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class DepositResponse {

    private UUID id;
    private String transactionHash;
    private String chain;
    private String memo;
    private BigDecimal amountUsd;
    private BigDecimal exactAmountUsd;  // ✅ NEW: Add exact amount
    private BigDecimal pointsIssued;
    private String status;
    private String failureReason;
    private Instant submittedAt;
    private Instant confirmedAt;

    // Default constructor
    public DepositResponse() {}

    // ✅ NEW: Constructor from PointDeposit entity
    public DepositResponse(PointDeposit deposit) {
        this.id = deposit.getId();
        this.transactionHash = deposit.getTransactionHash();
        this.chain = deposit.getChain();
        this.memo = deposit.getMemo();
        this.amountUsd = deposit.getAmountUsd();
        this.exactAmountUsd = deposit.getExactAmountUsd();
        this.pointsIssued = deposit.getPointsIssued();
        this.status = deposit.getStatus();
        this.failureReason = deposit.getFailureReason();
        this.submittedAt = deposit.getSubmittedAt();
        this.confirmedAt = deposit.getConfirmedAt();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTransactionHash() { return transactionHash; }
    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getChain() { return chain; }
    public void setChain(String chain) { this.chain = chain; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public BigDecimal getAmountUsd() { return amountUsd; }
    public void setAmountUsd(BigDecimal amountUsd) { this.amountUsd = amountUsd; }

    // ✅ NEW: Exact amount getter/setter
    public BigDecimal getExactAmountUsd() { return exactAmountUsd; }
    public void setExactAmountUsd(BigDecimal exactAmountUsd) {
        this.exactAmountUsd = exactAmountUsd;
    }

    public BigDecimal getPointsIssued() { return pointsIssued; }
    public void setPointsIssued(BigDecimal pointsIssued) {
        this.pointsIssued = pointsIssued;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
}