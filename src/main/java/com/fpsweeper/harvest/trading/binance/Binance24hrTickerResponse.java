package com.fpsweeper.harvest.trading.binance;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public class Binance24hrTickerResponse {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("priceChange")
    private BigDecimal priceChange;

    @JsonProperty("priceChangePercent")
    private BigDecimal priceChangePercent;

    @JsonProperty("lastPrice")
    private BigDecimal lastPrice;

    @JsonProperty("volume")
    private BigDecimal volume;

    @JsonProperty("quoteVolume")
    private BigDecimal quoteVolume;

    @JsonProperty("highPrice")
    private BigDecimal highPrice;

    @JsonProperty("lowPrice")
    private BigDecimal lowPrice;

    // Constructors
    public Binance24hrTickerResponse() {
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public BigDecimal getPriceChange() {
        return priceChange;
    }

    public void setPriceChange(BigDecimal priceChange) {
        this.priceChange = priceChange;
    }

    public BigDecimal getPriceChangePercent() {
        return priceChangePercent;
    }

    public void setPriceChangePercent(BigDecimal priceChangePercent) {
        this.priceChangePercent = priceChangePercent;
    }

    public BigDecimal getLastPrice() {
        return lastPrice;
    }

    public void setLastPrice(BigDecimal lastPrice) {
        this.lastPrice = lastPrice;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getQuoteVolume() {
        return quoteVolume;
    }

    public void setQuoteVolume(BigDecimal quoteVolume) {
        this.quoteVolume = quoteVolume;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public void setHighPrice(BigDecimal highPrice) {
        this.highPrice = highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public void setLowPrice(BigDecimal lowPrice) {
        this.lowPrice = lowPrice;
    }
}