package com.fpsweeper.harvest.social.dto;

import com.fpsweeper.harvest.social.DiscordAccounts;

public class DiscordAccountResponse {
    private String discordId;
    private String username;
    private String discriminator;
    private String displayName;
    private String avatarUrl;
    private String linkedAt;

    public DiscordAccountResponse(DiscordAccounts account) {
        this.discordId = account.getDiscordId();
        this.username = account.getUsername();
        this.discriminator = account.getDiscriminator();
        this.displayName = account.getDisplayName();
        this.avatarUrl = account.getAvatarUrl();
        this.linkedAt = account.getLinkedAt().toString();
    }

    // Getters and Setters
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

    public String getLinkedAt() { return linkedAt; }
    public void setLinkedAt(String linkedAt) { this.linkedAt = linkedAt; }
}