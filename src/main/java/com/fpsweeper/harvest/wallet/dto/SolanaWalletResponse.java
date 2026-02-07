package com.fpsweeper.harvest.wallet.dto;
import com.fpsweeper.harvest.wallet.SolanaWallets;

import java.time.Instant;

public class SolanaWalletResponse {

    private String walletAddress;
    private Boolean isVerified;
    private Instant linkedAt;
    private String nickname;
    private Boolean isPrimary;

    // Constructors
    public SolanaWalletResponse() {
    }

    public SolanaWalletResponse(SolanaWallets wallet) {
        this.walletAddress = wallet.getWalletAddress();
        this.isVerified = wallet.isVerified();
        this.linkedAt = wallet.getLinkedAt();
        this.nickname = wallet.getNickname();
        this.isPrimary = wallet.isPrimary();
    }

    // Getters and Setters
    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}