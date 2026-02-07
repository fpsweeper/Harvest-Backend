package com.fpsweeper.harvest.wallet.dto;

public record SolanaVerifyWalletRequest(
        String walletAddress,
        String message,
        String signature
) {}
