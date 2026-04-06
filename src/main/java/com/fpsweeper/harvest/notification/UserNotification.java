package com.fpsweeper.harvest.notification;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_notifications", indexes = {
        @Index(name = "idx_notif_user_id", columnList = "user_id"),
        @Index(name = "idx_notif_created_at", columnList = "created_at")
})
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // Optional metadata — bot name, symbol, amount, etc.
    @Column(name = "bot_name", length = 100)
    private String botName;

    @Column(length = 20)
    private String symbol;

    @Column(precision = 18, scale = 2)
    private java.math.BigDecimal amount;

    public UserNotification() {}

    public UserNotification(UUID userId, NotificationType type, String title, String message) {
        this.userId  = userId;
        this.type    = type;
        this.title   = title;
        this.message = message;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public UUID getId()             { return id; }
    public UUID getUserId()         { return userId; }
    public void setUserId(UUID u)   { this.userId = u; }

    public NotificationType getType()        { return type; }
    public void setType(NotificationType t)  { this.type = t; }

    public String getTitle()          { return title; }
    public void setTitle(String t)    { this.title = t; }

    public String getMessage()        { return message; }
    public void setMessage(String m)  { this.message = m; }

    public boolean isRead()          { return read; }
    public void setRead(boolean r)   { this.read = r; }

    public Instant getCreatedAt()    { return createdAt; }

    public String getBotName()              { return botName; }
    public void setBotName(String b)        { this.botName = b; }

    public String getSymbol()               { return symbol; }
    public void setSymbol(String s)         { this.symbol = s; }

    public java.math.BigDecimal getAmount()              { return amount; }
    public void setAmount(java.math.BigDecimal a)        { this.amount = a; }
}