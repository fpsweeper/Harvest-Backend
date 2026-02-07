package com.fpsweeper.harvest.social;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "oauth_linking_tokens")
public class OAuthLinkingToken {

    @Id
    @Column(length = 255)
    private String token;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public OAuthLinkingToken(String userEmail) {
        this.token = UUID.randomUUID().toString();
        this.userEmail = userEmail;
        this.expiresAt = Instant.now().plusSeconds(600); // 10 minutes
    }

    // Constructors
    public OAuthLinkingToken() {}

    public OAuthLinkingToken(String token, String userEmail, Instant expiresAt) {
        this.token = token;
        this.userEmail = userEmail;
        this.expiresAt = expiresAt;
    }

    // Getters and Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}