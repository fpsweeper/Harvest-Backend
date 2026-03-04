package com.fpsweeper.harvest.points;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "supported_chains")
public class SupportedChain {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chain_name", nullable = false, unique = true)
    private String chainName;

    @Column(name = "platform_wallet_address", nullable = false)
    private String platformWalletAddress;

    @Column(name = "usdc_token_address")
    private String usdcTokenAddress;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "min_deposit_usd", precision = 10, scale = 2)
    private BigDecimal minDepositUsd;

    @Column(name = "confirmation_blocks_required")
    private Integer confirmationBlocksRequired;

    @Column(name = "rpc_endpoint")
    private String rpcEndpoint;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // Constructors
    public SupportedChain() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getChainName() { return chainName; }
    public void setChainName(String chainName) { this.chainName = chainName; }

    public String getPlatformWalletAddress() { return platformWalletAddress; }
    public void setPlatformWalletAddress(String platformWalletAddress) {
        this.platformWalletAddress = platformWalletAddress;
    }

    public String getUsdcTokenAddress() { return usdcTokenAddress; }
    public void setUsdcTokenAddress(String usdcTokenAddress) {
        this.usdcTokenAddress = usdcTokenAddress;
    }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public BigDecimal getMinDepositUsd() { return minDepositUsd; }
    public void setMinDepositUsd(BigDecimal minDepositUsd) {
        this.minDepositUsd = minDepositUsd;
    }

    public Integer getConfirmationBlocksRequired() {
        return confirmationBlocksRequired;
    }
    public void setConfirmationBlocksRequired(Integer confirmationBlocksRequired) {
        this.confirmationBlocksRequired = confirmationBlocksRequired;
    }

    public String getRpcEndpoint() { return rpcEndpoint; }
    public void setRpcEndpoint(String rpcEndpoint) {
        this.rpcEndpoint = rpcEndpoint;
    }

    public Instant getCreatedAt() { return createdAt; }
}