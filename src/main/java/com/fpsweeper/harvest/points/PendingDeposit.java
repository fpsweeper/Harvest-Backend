package com.fpsweeper.harvest.points;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pending_deposits")
public class PendingDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "chain", nullable = false, length = 50)
    private String chain;

    @Column(name = "base_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "security_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal securityAmount;

    @Column(name = "exact_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal exactAmount;

    @Column(name = "points_to_receive", nullable = false, precision = 10, scale = 2)
    private BigDecimal pointsToReceive;

    @Column(name = "platform_wallet", nullable = false)
    private String platformWallet;

    @Column(name = "token_address", nullable = false)
    private String tokenAddress;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "submitted", nullable = false)
    private Boolean submitted = false;

    @Column(name = "transaction_hash")
    private String transactionHash;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (expiresAt == null) {
            // Default: 24 hours from creation
            expiresAt = createdAt.plusSeconds(86400);
        }
    }

    // Constructors
    public PendingDeposit() {}

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(BigDecimal baseAmount) {
        this.baseAmount = baseAmount;
    }

    public BigDecimal getSecurityAmount() {
        return securityAmount;
    }

    public void setSecurityAmount(BigDecimal securityAmount) {
        this.securityAmount = securityAmount;
    }

    public BigDecimal getExactAmount() {
        return exactAmount;
    }

    public void setExactAmount(BigDecimal exactAmount) {
        this.exactAmount = exactAmount;
    }

    public BigDecimal getPointsToReceive() {
        return pointsToReceive;
    }

    public void setPointsToReceive(BigDecimal pointsToReceive) {
        this.pointsToReceive = pointsToReceive;
    }

    public String getPlatformWallet() {
        return platformWallet;
    }

    public void setPlatformWallet(String platformWallet) {
        this.platformWallet = platformWallet;
    }

    public String getTokenAddress() {
        return tokenAddress;
    }

    public void setTokenAddress(String tokenAddress) {
        this.tokenAddress = tokenAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Boolean getSubmitted() {
        return submitted;
    }

    public void setSubmitted(Boolean submitted) {
        this.submitted = submitted;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    // Helper methods
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public long getTimeRemainingSeconds() {
        long remaining = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }
}