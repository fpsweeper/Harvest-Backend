package com.fpsweeper.harvest.points.dto;

import com.fpsweeper.harvest.points.PendingDeposit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PendingDepositResponse {

    private UUID id;
    private String chain;
    private BigDecimal baseAmount;
    private BigDecimal securityAmount;
    private BigDecimal exactAmount;
    private BigDecimal pointsToReceive;
    private String platformWallet;
    private String tokenAddress;
    private Instant createdAt;
    private Instant expiresAt;
    private Long timeRemainingSeconds;
    private Boolean expired;

    public PendingDepositResponse() {}

    public PendingDepositResponse(PendingDeposit pending) {
        this.id = pending.getId();
        this.chain = pending.getChain();
        this.baseAmount = pending.getBaseAmount();
        this.securityAmount = pending.getSecurityAmount();
        this.exactAmount = pending.getExactAmount();
        this.pointsToReceive = pending.getPointsToReceive();
        this.platformWallet = pending.getPlatformWallet();
        this.tokenAddress = pending.getTokenAddress();
        this.createdAt = pending.getCreatedAt();
        this.expiresAt = pending.getExpiresAt();
        this.timeRemainingSeconds = pending.getTimeRemainingSeconds();
        this.expired = pending.isExpired();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getChain() { return chain; }
    public void setChain(String chain) { this.chain = chain; }

    public BigDecimal getBaseAmount() { return baseAmount; }
    public void setBaseAmount(BigDecimal baseAmount) { this.baseAmount = baseAmount; }

    public BigDecimal getSecurityAmount() { return securityAmount; }
    public void setSecurityAmount(BigDecimal securityAmount) { this.securityAmount = securityAmount; }

    public BigDecimal getExactAmount() { return exactAmount; }
    public void setExactAmount(BigDecimal exactAmount) { this.exactAmount = exactAmount; }

    public BigDecimal getPointsToReceive() { return pointsToReceive; }
    public void setPointsToReceive(BigDecimal pointsToReceive) { this.pointsToReceive = pointsToReceive; }

    public String getPlatformWallet() { return platformWallet; }
    public void setPlatformWallet(String platformWallet) { this.platformWallet = platformWallet; }

    public String getTokenAddress() { return tokenAddress; }
    public void setTokenAddress(String tokenAddress) { this.tokenAddress = tokenAddress; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Long getTimeRemainingSeconds() { return timeRemainingSeconds; }
    public void setTimeRemainingSeconds(Long timeRemainingSeconds) { this.timeRemainingSeconds = timeRemainingSeconds; }

    public Boolean getExpired() { return expired; }
    public void setExpired(Boolean expired) { this.expired = expired; }
}