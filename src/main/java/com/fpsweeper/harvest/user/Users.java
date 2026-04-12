package com.fpsweeper.harvest.user;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
public class Users {
    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "auth_provider", nullable = false)
    private String authProvider = "LOCAL"; // LOCAL, GOOGLE

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "role", nullable = false)
    private String role = "USER";

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    @Basic
    @Column(name = "twitter_id")
    private String twitterId;
    @Basic
    @Column(name = "twitter_handle")
    private String twitterHandle;

    @Column(name = "discord_id")  // ✅ Add this
    private String discordId;

    /**
     * Maximum total virtual credit this user can allocate across simulation bots.
     * Default: $1,000. Admin can raise this per user.
     */
    @Column(name = "simulation_credit_limit", nullable = false, precision = 10, scale = 2)
    private BigDecimal simulationCreditLimit = new BigDecimal("1000.00");

    public String getDiscordId() {
        return discordId;
    }

    public void setDiscordId(String discordId) {
        this.discordId = discordId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Users users = (Users) o;
        return emailVerified == users.emailVerified && Objects.equals(id, users.id) && Objects.equals(email, users.email) && Objects.equals(passwordHash, users.passwordHash) && Objects.equals(authProvider, users.authProvider) && Objects.equals(providerId, users.providerId) && Objects.equals(status, users.status) && Objects.equals(createdAt, users.createdAt) && Objects.equals(updatedAt, users.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, passwordHash, emailVerified, authProvider, providerId, status, createdAt, updatedAt);
    }

    public String getTwitterId() {
        return twitterId;
    }

    public void setTwitterId(String twitterId) {
        this.twitterId = twitterId;
    }

    public String getTwitterHandle() {
        return twitterHandle;
    }

    public void setTwitterHandle(String twitterHandle) {
        this.twitterHandle = twitterHandle;
    }

    public BigDecimal getSimulationCreditLimit() {
        return simulationCreditLimit != null ? simulationCreditLimit : new BigDecimal("1000.00");
    }

    public void setSimulationCreditLimit(BigDecimal simulationCreditLimit) {
        this.simulationCreditLimit = simulationCreditLimit;
    }
}