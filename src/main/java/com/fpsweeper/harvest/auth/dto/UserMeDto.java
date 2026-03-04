package com.fpsweeper.harvest.auth.dto;

import com.fpsweeper.harvest.wallet.UserWallet;
import com.fpsweeper.harvest.wallet.dto.SolanaWalletResponse;

import java.util.UUID;

public class UserMeDto {

    private UUID id;
    private String email;
    private String role;
    private String authProvider;
    private SolanaWalletResponse wallet;  // Wallet response

    // Constructors
    public UserMeDto() {}

    public UserMeDto(UUID id, String email, String role, String authProvider, UserWallet wallet) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.authProvider = authProvider;
        // ✅ Convert entity to DTO (includes chain now)
        this.wallet = wallet != null ? new SolanaWalletResponse(wallet) : null;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public SolanaWalletResponse getWallet() {
        return wallet;
    }

    public void setWallet(SolanaWalletResponse wallet) {
        this.wallet = wallet;
    }
}