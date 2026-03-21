package com.fpsweeper.harvest.trading;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bot_positions")
public class BotPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bot_id", nullable = false)
    private UUID botId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal quantity;

    @Column(name = "entry_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal entryPrice;

    @Column(name = "entry_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal entryValue;

    @Column(name = "entry_trade_id")
    private UUID entryTradeId;

    @Column(name = "current_price", precision = 18, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "current_value", precision = 18, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "unrealized_pnl", precision = 18, scale = 2)
    private BigDecimal unrealizedPnl;

    @Column(name = "unrealized_pnl_percentage", precision = 8, scale = 4)
    private BigDecimal unrealizedPnlPercentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PositionStatus status = PositionStatus.OPEN;

    @Column(name = "exit_price", precision = 18, scale = 2)
    private BigDecimal exitPrice;

    @Column(name = "exit_value", precision = 18, scale = 2)
    private BigDecimal exitValue;

    @Column(name = "exit_trade_id")
    private UUID exitTradeId;

    @Column(name = "realized_pnl", precision = 18, scale = 2)
    private BigDecimal realizedPnl;

    @Column(name = "realized_pnl_percentage", precision = 8, scale = 4)
    private BigDecimal realizedPnlPercentage;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // Constructors
    public BotPosition() {
    }

    public BotPosition(UUID botId, String symbol, BigDecimal quantity, BigDecimal entryPrice) {
        this.botId = botId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.entryPrice = entryPrice;
        this.entryValue = quantity.multiply(entryPrice);
    }

    // Helper methods
    public boolean isOpen() {
        return status == PositionStatus.OPEN;
    }

    public boolean isClosed() {
        return status == PositionStatus.CLOSED;
    }

    /**
     * Update unrealized P&L based on current market price
     */
    public void updateUnrealizedPnl(BigDecimal currentMarketPrice) {
        if (!isOpen()) {
            return;
        }

        this.currentPrice = currentMarketPrice;
        this.currentValue = quantity.multiply(currentMarketPrice);
        this.unrealizedPnl = currentValue.subtract(entryValue);

        if (entryValue.compareTo(BigDecimal.ZERO) > 0) {
            this.unrealizedPnlPercentage = unrealizedPnl
                    .divide(entryValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        this.updatedAt = Instant.now();
    }

    /**
     * Close position and calculate realized P&L
     */
    public void closePosition(BigDecimal exitMarketPrice, UUID exitTradeId) {
        this.exitPrice = exitMarketPrice;
        this.exitValue = quantity.multiply(exitMarketPrice);
        this.exitTradeId = exitTradeId;
        this.realizedPnl = exitValue.subtract(entryValue);

        if (entryValue.compareTo(BigDecimal.ZERO) > 0) {
            this.realizedPnlPercentage = realizedPnl
                    .divide(entryValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        this.status = PositionStatus.CLOSED;
        this.closedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBotId() {
        return botId;
    }

    public void setBotId(UUID botId) {
        this.botId = botId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    public BigDecimal getEntryValue() {
        return entryValue;
    }

    public void setEntryValue(BigDecimal entryValue) {
        this.entryValue = entryValue;
    }

    public UUID getEntryTradeId() {
        return entryTradeId;
    }

    public void setEntryTradeId(UUID entryTradeId) {
        this.entryTradeId = entryTradeId;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public void setUnrealizedPnl(BigDecimal unrealizedPnl) {
        this.unrealizedPnl = unrealizedPnl;
    }

    public BigDecimal getUnrealizedPnlPercentage() {
        return unrealizedPnlPercentage;
    }

    public void setUnrealizedPnlPercentage(BigDecimal unrealizedPnlPercentage) {
        this.unrealizedPnlPercentage = unrealizedPnlPercentage;
    }

    public PositionStatus getStatus() {
        return status;
    }

    public void setStatus(PositionStatus status) {
        this.status = status;
    }

    public BigDecimal getExitPrice() {
        return exitPrice;
    }

    public void setExitPrice(BigDecimal exitPrice) {
        this.exitPrice = exitPrice;
    }

    public BigDecimal getExitValue() {
        return exitValue;
    }

    public void setExitValue(BigDecimal exitValue) {
        this.exitValue = exitValue;
    }

    public UUID getExitTradeId() {
        return exitTradeId;
    }

    public void setExitTradeId(UUID exitTradeId) {
        this.exitTradeId = exitTradeId;
    }

    public BigDecimal getRealizedPnl() {
        return realizedPnl;
    }

    public void setRealizedPnl(BigDecimal realizedPnl) {
        this.realizedPnl = realizedPnl;
    }

    public BigDecimal getRealizedPnlPercentage() {
        return realizedPnlPercentage;
    }

    public void setRealizedPnlPercentage(BigDecimal realizedPnlPercentage) {
        this.realizedPnlPercentage = realizedPnlPercentage;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}