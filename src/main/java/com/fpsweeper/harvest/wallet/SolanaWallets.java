package com.fpsweeper.harvest.wallet;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "solana_wallets", schema = "public", catalog = "postgres")
public class SolanaWallets {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "wallet_address", nullable = false, unique = true, length = 44)
    private String walletAddress;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = true;

    public SolanaWallets(UUID userId, String walletAddress) {
        this.userId = userId;
        this.walletAddress = walletAddress;
        this.linkedAt = Instant.now();
        this.isVerified = false;
        this.isPrimary = true;
    }

    public SolanaWallets() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }

    public Instant getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Instant lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SolanaWallets that = (SolanaWallets) o;
        return isVerified == that.isVerified && isPrimary == that.isPrimary && Objects.equals(id, that.id) && Objects.equals(userId, that.userId) && Objects.equals(walletAddress, that.walletAddress) && Objects.equals(linkedAt, that.linkedAt) && Objects.equals(lastVerifiedAt, that.lastVerifiedAt) && Objects.equals(nickname, that.nickname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, walletAddress, isVerified, linkedAt, lastVerifiedAt, nickname, isPrimary);
    }
}
