package com.fpsweeper.harvest.points.dto;

import jakarta.validation.constraints.NotBlank;

public class WalletLinkRequest {

    @NotBlank(message = "Chain is required")
    private String chain;

    @NotBlank(message = "Wallet address is required")
    private String walletAddress;

    // Optional: signature for verification
    private String signature;

    // Constructors
    public WalletLinkRequest() {}

    public WalletLinkRequest(String chain, String walletAddress) {
        this.chain = chain;
        this.walletAddress = walletAddress;
    }

    // Getters and Setters
    public String getChain() { return chain; }
    public void setChain(String chain) { this.chain = chain; }

    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
}