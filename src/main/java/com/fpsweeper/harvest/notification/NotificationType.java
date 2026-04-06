package com.fpsweeper.harvest.notification;

public enum NotificationType {
    // Trade events
    BOT_BUY,
    BOT_SELL,
    BOT_TAKE_PROFIT,
    BOT_STOP_LOSS,

    // Bot lifecycle
    BOT_STARTED,
    BOT_PAUSED,
    BOT_STOPPED,
    BOT_AUTO_PAUSED,   // paused due to insufficient points

    // Points & wallet
    DEPOSIT_SUCCESS,
    LOW_POINTS,        // balance < 10 pts

    // System
    ERROR
}