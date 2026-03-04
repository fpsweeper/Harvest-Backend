package com.fpsweeper.harvest.points;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "point_deposits")
public class PointDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "transaction_hash", nullable = false, unique = true)
    private String transactionHash;

    @Column(nullable = false)
    private String chain;

    // ✅ NEW: Memo field
    @Column(length = 100)
    private String memo;

    @Column(name = "from_wallet")
    private String fromWallet;

    @Column(name = "amount_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountUsd;

    @Column(name = "exact_amount_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal exactAmountUsd;

    @Column(name = "points_issued", precision = 10, scale = 2, nullable = false)
    private BigDecimal pointsIssued;

    @Column(name = "conversion_rate", precision = 10, scale = 4, nullable = false)
    private BigDecimal conversionRate;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, VERIFIED, CONFIRMED, FAILED

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now();

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "confirmations")
    private Integer confirmations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        submittedAt = Instant.now();
    }

    public BigDecimal getExactAmountUsd() {
        return exactAmountUsd;
    }

    public void setExactAmountUsd(BigDecimal exactAmountUsd) {
        this.exactAmountUsd = exactAmountUsd;
    }

    // Constructors
    public PointDeposit() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTransactionHash() { return transactionHash; }
    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getChain() { return chain; }
    public void setChain(String chain) { this.chain = chain; }

    // ✅ NEW: Memo getter/setter
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public String getFromWallet() { return fromWallet; }
    public void setFromWallet(String fromWallet) {
        this.fromWallet = fromWallet;
    }

    public BigDecimal getAmountUsd() { return amountUsd; }
    public void setAmountUsd(BigDecimal amountUsd) {
        this.amountUsd = amountUsd;
    }

    public BigDecimal getPointsIssued() { return pointsIssued; }
    public void setPointsIssued(BigDecimal pointsIssued) {
        this.pointsIssued = pointsIssued;
    }

    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Long getBlockNumber() { return blockNumber; }
    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public Integer getConfirmations() { return confirmations; }
    public void setConfirmations(Integer confirmations) {
        this.confirmations = confirmations;
    }

    public Instant getCreatedAt() { return createdAt; }
}