package com.fpsweeper.harvest.points.dto;

import java.time.Instant;
import java.util.UUID;

public class WalletResponse {

    private UUID id;
    private String chain;
    private String walletAddress;
    private Boolean isVerified;
    private Instant createdAt;

    // Constructors
    public WalletResponse() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getChain() { return chain; }
    public void setChain(String chain) { this.chain = chain; }

    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}