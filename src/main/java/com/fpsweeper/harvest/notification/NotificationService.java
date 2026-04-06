package com.fpsweeper.harvest.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final int MAX_NOTIFICATIONS = 50;

    @Autowired
    private UserNotificationRepository repository;

    // ── Trade events ───────────────────────────────────────────────────────────

    @Async
    @Transactional
    public void notifyBotBuy(UUID userId, String botName, String symbol, BigDecimal amount, BigDecimal price) {
        String msg = String.format("%s bought %.6f %s at $%.2f (total: $%.2f)",
                botName, amount, symbol, price, amount.multiply(price));
        create(userId, NotificationType.BOT_BUY, "🟢 Buy Order Executed", msg, botName, symbol, amount);
    }

    @Async
    @Transactional
    public void notifyBotSell(UUID userId, String botName, String symbol, BigDecimal amount,
                              BigDecimal price, BigDecimal pnl) {
        String pnlStr = pnl != null
                ? String.format(" | P&L: %s$%.2f", pnl.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", pnl)
                : "";
        String msg = String.format("%s sold %.6f %s at $%.2f%s", botName, amount, symbol, price, pnlStr);
        create(userId, NotificationType.BOT_SELL, "🔴 Sell Order Executed", msg, botName, symbol, amount);
    }

    @Async
    @Transactional
    public void notifyTakeProfit(UUID userId, String botName, String symbol, BigDecimal pnl, BigDecimal pnlPct) {
        String msg = String.format("%s hit take profit on %s — +$%.2f (+%.2f%%)", botName, symbol, pnl, pnlPct);
        create(userId, NotificationType.BOT_TAKE_PROFIT, "🎯 Take Profit Hit", msg, botName, symbol, pnl);
    }

    @Async
    @Transactional
    public void notifyStopLoss(UUID userId, String botName, String symbol, BigDecimal pnl, BigDecimal pnlPct) {
        String msg = String.format("%s stop loss triggered on %s — $%.2f (%.2f%%)", botName, symbol, pnl, pnlPct);
        create(userId, NotificationType.BOT_STOP_LOSS, "🛑 Stop Loss Triggered", msg, botName, symbol, pnl);
    }

    // ── Bot lifecycle ──────────────────────────────────────────────────────────

    @Async
    @Transactional
    public void notifyBotStarted(UUID userId, String botName, String symbol, String timeframe) {
        String msg = String.format("%s started trading %s on %s timeframe", botName, symbol, timeframe);
        create(userId, NotificationType.BOT_STARTED, "▶️ Bot Started", msg, botName, symbol, null);
    }

    @Async
    @Transactional
    public void notifyBotPaused(UUID userId, String botName) {
        String msg = String.format("%s has been paused", botName);
        create(userId, NotificationType.BOT_PAUSED, "⏸️ Bot Paused", msg, botName, null, null);
    }

    @Async
    @Transactional
    public void notifyBotStopped(UUID userId, String botName) {
        String msg = String.format("%s has been stopped and all positions closed", botName);
        create(userId, NotificationType.BOT_STOPPED, "⏹️ Bot Stopped", msg, botName, null, null);
    }

    @Async
    @Transactional
    public void notifyBotAutoPaused(UUID userId, String botName) {
        String msg = String.format("%s was automatically paused — insufficient points balance", botName);
        create(userId, NotificationType.BOT_AUTO_PAUSED, "⚠️ Bot Auto-Paused", msg, botName, null, null);
    }

    // ── Points & wallet ────────────────────────────────────────────────────────

    @Async
    @Transactional
    public void notifyDepositSuccess(UUID userId, BigDecimal points) {
        String msg = String.format("%.2f points have been added to your account", points);
        create(userId, NotificationType.DEPOSIT_SUCCESS, "💰 Deposit Successful", msg, null, null, points);
    }

    @Async
    @Transactional
    public void notifyLowPoints(UUID userId, BigDecimal remaining) {
        String msg = String.format("You have %.2f points remaining — top up to keep your bots running", remaining);
        create(userId, NotificationType.LOW_POINTS, "⚠️ Low Points Balance", msg, null, null, remaining);
    }

    // ── Read / management ──────────────────────────────────────────────────────

    public List<UserNotification> getNotifications(UUID userId, int limit) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
    }

    public long getUnreadCount(UUID userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAllRead(UUID userId) {
        repository.markAllAsRead(userId);
    }

    @Transactional
    public void markRead(UUID notificationId, UUID userId) {
        repository.markAsRead(notificationId, userId);
    }

    @Transactional
    public void clearAll(UUID userId) {
        repository.deleteByUserId(userId);
    }

    // ── Internal ───────────────────────────────────────────────────────────────

    private void create(UUID userId, NotificationType type, String title, String message,
                        String botName, String symbol, BigDecimal amount) {
        try {
            UserNotification n = new UserNotification(userId, type, title, message);
            n.setBotName(botName);
            n.setSymbol(symbol);
            n.setAmount(amount);
            repository.save(n);
            log.debug("🔔 Notification created for user {}: {}", userId, title);
        } catch (Exception e) {
            // Never let notification failure break the main flow
            log.error("❌ Failed to create notification for user {}: {}", userId, e.getMessage());
        }
    }
}