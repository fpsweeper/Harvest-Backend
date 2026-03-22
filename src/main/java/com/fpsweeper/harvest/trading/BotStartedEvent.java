package com.fpsweeper.harvest.trading;

public class BotStartedEvent {
    private final java.util.UUID botId;
    public BotStartedEvent(java.util.UUID botId) { this.botId = botId; }
    public java.util.UUID getBotId() { return botId; }
}