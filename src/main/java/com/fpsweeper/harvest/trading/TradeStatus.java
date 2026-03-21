package com.fpsweeper.harvest.trading;

public enum TradeStatus {
    PENDING,    // Trade submitted but not yet filled
    FILLED,     // Trade completed successfully
    CANCELLED,  // Trade cancelled before execution
    FAILED      // Trade execution failed
}