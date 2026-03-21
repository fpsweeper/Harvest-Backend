package com.fpsweeper.harvest.trading.binance;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.List;

/**
 * Binance Kline (Candlestick) Response
 * Format: [
 *   [
 *     1499040000000,      // 0: Open time
 *     "0.01634000",       // 1: Open
 *     "0.80000000",       // 2: High
 *     "0.01575800",       // 3: Low
 *     "0.01577100",       // 4: Close
 *     "148976.11427815",  // 5: Volume
 *     1499644799999,      // 6: Close time
 *     "2434.19055334",    // 7: Quote asset volume
 *     308,                // 8: Number of trades
 *     "1756.87402397",    // 9: Taker buy base asset volume
 *     "28.46694368",      // 10: Taker buy quote asset volume
 *     "0"                 // 11: Ignore
 *   ]
 * ]
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class BinanceKlineResponse {

    private Long openTime;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private Long closeTime;
    private BigDecimal quoteAssetVolume;
    private Integer numberOfTrades;
    private BigDecimal takerBuyBaseAssetVolume;
    private BigDecimal takerBuyQuoteAssetVolume;
    private String ignore;

    // Constructors
    public BinanceKlineResponse() {
    }

    // Constructor from array (Binance returns arrays)
    public BinanceKlineResponse(List<Object> data) {
        if (data != null && data.size() >= 11) {
            this.openTime = parseLong(data.get(0));
            this.open = parseBigDecimal(data.get(1));
            this.high = parseBigDecimal(data.get(2));
            this.low = parseBigDecimal(data.get(3));
            this.close = parseBigDecimal(data.get(4));
            this.volume = parseBigDecimal(data.get(5));
            this.closeTime = parseLong(data.get(6));
            this.quoteAssetVolume = parseBigDecimal(data.get(7));
            this.numberOfTrades = parseInt(data.get(8));
            this.takerBuyBaseAssetVolume = parseBigDecimal(data.get(9));
            this.takerBuyQuoteAssetVolume = parseBigDecimal(data.get(10));
            this.ignore = data.size() > 11 ? String.valueOf(data.get(11)) : "0";
        }
    }

    // Helper parsing methods
    private Long parseLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private Integer parseInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    // Getters and Setters
    public Long getOpenTime() {
        return openTime;
    }

    public void setOpenTime(Long openTime) {
        this.openTime = openTime;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public void setOpen(BigDecimal open) {
        this.open = open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public void setHigh(BigDecimal high) {
        this.high = high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public void setLow(BigDecimal low) {
        this.low = low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public void setClose(BigDecimal close) {
        this.close = close;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public Long getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(Long closeTime) {
        this.closeTime = closeTime;
    }

    public BigDecimal getQuoteAssetVolume() {
        return quoteAssetVolume;
    }

    public void setQuoteAssetVolume(BigDecimal quoteAssetVolume) {
        this.quoteAssetVolume = quoteAssetVolume;
    }

    public Integer getNumberOfTrades() {
        return numberOfTrades;
    }

    public void setNumberOfTrades(Integer numberOfTrades) {
        this.numberOfTrades = numberOfTrades;
    }

    public BigDecimal getTakerBuyBaseAssetVolume() {
        return takerBuyBaseAssetVolume;
    }

    public void setTakerBuyBaseAssetVolume(BigDecimal takerBuyBaseAssetVolume) {
        this.takerBuyBaseAssetVolume = takerBuyBaseAssetVolume;
    }

    public BigDecimal getTakerBuyQuoteAssetVolume() {
        return takerBuyQuoteAssetVolume;
    }

    public void setTakerBuyQuoteAssetVolume(BigDecimal takerBuyQuoteAssetVolume) {
        this.takerBuyQuoteAssetVolume = takerBuyQuoteAssetVolume;
    }

    public String getIgnore() {
        return ignore;
    }

    public void setIgnore(String ignore) {
        this.ignore = ignore;
    }

    @Override
    public String toString() {
        return "BinanceKlineResponse{" +
                "openTime=" + openTime +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                ", closeTime=" + closeTime +
                '}';
    }
}