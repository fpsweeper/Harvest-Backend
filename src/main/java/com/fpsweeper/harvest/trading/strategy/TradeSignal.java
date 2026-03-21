package com.fpsweeper.harvest.trading.strategy;

import com.fpsweeper.harvest.trading.TradeType;

import java.math.BigDecimal;
import java.util.Map;

public class TradeSignal {

    private final TradeType action;
    private final String symbol;
    private final BigDecimal amount;
    private final String reason;
    private final Map<String, BigDecimal> indicators; // ✅ snapshot at signal time

    private TradeSignal(TradeType action, String symbol, BigDecimal amount,
                        String reason, Map<String, BigDecimal> indicators) {
        this.action     = action;
        this.symbol     = symbol;
        this.amount     = amount;
        this.reason     = reason;
        this.indicators = indicators;
    }

    // ─── Factory methods ───────────────────────────────────────────────────────

    public static TradeSignal buy(String symbol, BigDecimal amount, String reason,
                                  Map<String, BigDecimal> indicators) {
        return new TradeSignal(TradeType.BUY, symbol, amount, reason, indicators);
    }

    public static TradeSignal sell(String symbol, BigDecimal amount, String reason,
                                   Map<String, BigDecimal> indicators) {
        return new TradeSignal(TradeType.SELL, symbol, amount, reason, indicators);
    }

    public static TradeSignal hold(String reason) {
        return new TradeSignal(null, null, null, reason, null);
    }

    // ─── Convenience overloads (no indicators — e.g. stop-loss triggers) ──────

    public static TradeSignal buy(String symbol, BigDecimal amount, String reason) {
        return new TradeSignal(TradeType.BUY, symbol, amount, reason, null);
    }

    public static TradeSignal sell(String symbol, BigDecimal amount, String reason) {
        return new TradeSignal(TradeType.SELL, symbol, amount, reason, null);
    }

    // ─── Query methods ────────────────────────────────────────────────────────

    public boolean shouldTrade() { return action != null; }
    public boolean isBuy()       { return action == TradeType.BUY; }
    public boolean isSell()      { return action == TradeType.SELL; }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public TradeType getAction()                      { return action; }
    public String getSymbol()                         { return symbol; }
    public BigDecimal getAmount()                     { return amount; }
    public String getReason()                         { return reason; }
    public Map<String, BigDecimal> getIndicators()    { return indicators; }

    @Override
    public String toString() {
        if (action == null) return "HOLD: " + reason;
        return String.format("%s %s %s - %s", action, amount, symbol, reason);
    }
}