package com.fpsweeper.harvest.trading;

public enum BotStatus {
    CREATED,      // Bot configured but not started
    SIMULATING,   // Bot actively running
    PAUSED,       // Temporarily paused
    STOPPED,      // Permanently stopped
    DELETED       // Soft deleted
}