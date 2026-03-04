package com.fpsweeper.harvest.wallet;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solana_wallets")  // Keep same table name!
public class UserWallet {  // Keep same class name for now (backward compatibility)

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // ✅ NEW: Add chain field
    @Column(nullable = false)
    private String chain = "SOLANA";

    @Column(name = "wallet_address", nullable = false, unique = true)
    private String walletAddress;

    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt = Instant.now();

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(length = 50)
    private String nickname;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = true;

    @PrePersist
    protected void onCreate() {
        linkedAt = Instant.now();
        if (chain == null) {
            chain = "SOLANA";
        }
    }

    // Constructors
    public UserWallet() {}

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    // ✅ NEW: Chain getter/setter
    public String getChain() { return chain; }
    public void setChain(String chain) { this.chain = chain; }

    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Instant getLinkedAt() { return linkedAt; }
    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }

    public Instant getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(Instant lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
}