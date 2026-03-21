package com.fpsweeper.harvest.trading.binance;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class BinancePriceResponse {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("price")
    private BigDecimal price;

    // Constructors
    public BinancePriceResponse() {
    }

    public BinancePriceResponse(String symbol, BigDecimal price) {
        this.symbol = symbol;
        this.price = price;
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "BinancePriceResponse{" +
                "symbol='" + symbol + '\'' +
                ", price=" + price +
                '}';
    }
}