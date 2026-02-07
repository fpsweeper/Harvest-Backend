package com.fpsweeper.harvest.auth.dto;

import com.fpsweeper.harvest.wallet.SolanaWallets;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class UserMeDto {

    private UUID id;
    private String email;
    private String role;
    private String authProvider;
    private SolanaWallets wallet;

    public UserMeDto(UUID id, String email, String role, String authProvider, SolanaWallets wallet) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.authProvider = authProvider;
        this.wallet = wallet;
    }

    public SolanaWallets getWallet() {
        return wallet;
    }

    public void setWallets(SolanaWallets wallet) {
        this.wallet = wallet;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
