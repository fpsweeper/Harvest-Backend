package com.fpsweeper.harvest.wallet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LinkSolanaWalletRequest {

    @NotBlank(message = "Wallet address is required")
    @Pattern(
            regexp = "^[1-9A-HJ-NP-Za-km-z]{32,44}$",
            message = "Invalid Solana wallet address"
    )
    private String walletAddress;

    private String signature; // For wallet verification (optional)

    private String message; // Message that was signed (optional)

    private String nickname; // Optional wallet nickname

    // Constructors
    public LinkSolanaWalletRequest() {
    }

    public LinkSolanaWalletRequest(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    // Getters and Setters
    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}