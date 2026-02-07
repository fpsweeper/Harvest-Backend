package com.fpsweeper.harvest.social.dto;

import com.fpsweeper.harvest.social.TwitterAccounts;

import java.time.Instant;

public class TwitterAccountResponse {

    private String twitterId;
    private String username;
    private String displayName;
    private String profileImageUrl;
    private Instant linkedAt;

    public TwitterAccountResponse() {
    }

    public TwitterAccountResponse(TwitterAccounts account) {
        this.twitterId = account.getTwitterId();
        this.username = account.getUsername();
        this.displayName = account.getDisplayName();
        this.profileImageUrl = account.getProfileImageUrl();
        this.linkedAt = account.getLinkedAt();
    }

    // Getters and Setters
    public String getTwitterId() {
        return twitterId;
    }

    public void setTwitterId(String twitterId) {
        this.twitterId = twitterId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }
}