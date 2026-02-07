package com.fpsweeper.harvest.social;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "discord_accounts")
public class DiscordAccounts {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "discord_id", nullable = false, unique = true)
    private String discordId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "discriminator")
    private String discriminator;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "token_expires_at")
    private Instant tokenExpiresAt;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt = Instant.now();

    // Constructors
    public DiscordAccounts() {}

    public DiscordAccounts(UUID userId, String discordId, String username) {
        this.userId = userId;
        this.discordId = discordId;
        this.username = username;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getDiscordId() { return discordId; }
    public void setDiscordId(String discordId) { this.discordId = discordId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDiscriminator() { return discriminator; }
    public void setDiscriminator(String discriminator) { this.discriminator = discriminator; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public Instant getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(Instant tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public Instant getLinkedAt() { return linkedAt; }
    public void setLinkedAt(Instant linkedAt) { this.linkedAt = linkedAt; }
}