package com.fpsweeper.harvest.wallet.dto;

import com.fpsweeper.harvest.wallet.UserWallet;

import java.time.Instant;

public class SolanaWalletResponse {

    private String walletAddress;
    private Boolean verified;  // Changed from isVerified to match your entity
    private Instant linkedAt;
    private String nickname;
    private Boolean isPrimary;
    private String chain;  // ✅ NEW: Add chain field

    // Constructors
    public SolanaWalletResponse() {
    }

    public SolanaWalletResponse(UserWallet wallet) {
        this.walletAddress = wallet.getWalletAddress();
        this.verified = wallet.getIsVerified();
        this.linkedAt = wallet.getLinkedAt();
        this.nickname = wallet.getNickname();
        this.isPrimary = wallet.getIsPrimary();
        this.chain = wallet.getChain();  // ✅ NEW: Set chain
    }

    // Getters and Setters
    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
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

    // ✅ NEW: Chain getter/setter
    public String getChain() {
        return chain;
    }

    public void setChain(String chain) {
        this.chain = chain;
    }
}