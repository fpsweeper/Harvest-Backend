package com.fpsweeper.harvest.trading;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "market_data_cache", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"symbol", "timeframe", "open_time"})
})
public class MarketDataCache {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, length = 10)
    private String timeframe;

    // Candle Data (OHLCV)
    @Column(name = "open_time", nullable = false)
    private Instant openTime;

    @Column(name = "close_time", nullable = false)
    private Instant closeTime;

    @Column(name = "open_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal closePrice;

    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal volume;

    // Pre-calculated Indicators (for performance)
    @Column(name = "rsi_14", precision = 8, scale = 4)
    private BigDecimal rsi14;

    @Column(name = "rsi_7", precision = 8, scale = 4)
    private BigDecimal rsi7;

    @Column(precision = 18, scale = 8)
    private BigDecimal macd;

    @Column(name = "macd_signal", precision = 18, scale = 8)
    private BigDecimal macdSignal;

    @Column(name = "macd_histogram", precision = 18, scale = 8)
    private BigDecimal macdHistogram;

    @Column(name = "ma_20", precision = 18, scale = 2)
    private BigDecimal ma20;

    @Column(name = "ma_50", precision = 18, scale = 2)
    private BigDecimal ma50;

    @Column(name = "ma_100", precision = 18, scale = 2)
    private BigDecimal ma100;

    @Column(name = "ma_200", precision = 18, scale = 2)
    private BigDecimal ma200;

    @Column(name = "ema_12", precision = 18, scale = 2)
    private BigDecimal ema12;

    @Column(name = "ema_26", precision = 18, scale = 2)
    private BigDecimal ema26;

    @Column(name = "bb_upper", precision = 18, scale = 2)
    private BigDecimal bbUpper;

    @Column(name = "bb_middle", precision = 18, scale = 2)
    private BigDecimal bbMiddle;

    @Column(name = "bb_lower", precision = 18, scale = 2)
    private BigDecimal bbLower;

    @Column(name = "volume_ma", precision = 24, scale = 8)
    private BigDecimal volumeMa;

    // Metadata
    @Column(name = "data_source", length = 50)
    private String dataSource = "BINANCE";

    @Column(name = "is_complete")
    private Boolean isComplete = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    // Constructors
    public MarketDataCache() {
    }

    public MarketDataCache(String symbol, String timeframe, Instant openTime, Instant closeTime,
                           BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice,
                           BigDecimal closePrice, BigDecimal volume) {
        this.symbol = symbol;
        this.timeframe = timeframe;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTimeframe() {
        return timeframe;
    }

    public void setTimeframe(String timeframe) {
        this.timeframe = timeframe;
    }

    public Instant getOpenTime() {
        return openTime;
    }

    public void setOpenTime(Instant openTime) {
        this.openTime = openTime;
    }

    public Instant getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(Instant closeTime) {
        this.closeTime = closeTime;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public void setOpenPrice(BigDecimal openPrice) {
        this.openPrice = openPrice;
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

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public void setClosePrice(BigDecimal closePrice) {
        this.closePrice = closePrice;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public BigDecimal getRsi14() {
        return rsi14;
    }

    public void setRsi14(BigDecimal rsi14) {
        this.rsi14 = rsi14;
    }

    public BigDecimal getRsi7() {
        return rsi7;
    }

    public void setRsi7(BigDecimal rsi7) {
        this.rsi7 = rsi7;
    }

    public BigDecimal getMacd() {
        return macd;
    }

    public void setMacd(BigDecimal macd) {
        this.macd = macd;
    }

    public BigDecimal getMacdSignal() {
        return macdSignal;
    }

    public void setMacdSignal(BigDecimal macdSignal) {
        this.macdSignal = macdSignal;
    }

    public BigDecimal getMacdHistogram() {
        return macdHistogram;
    }

    public void setMacdHistogram(BigDecimal macdHistogram) {
        this.macdHistogram = macdHistogram;
    }

    public BigDecimal getMa20() {
        return ma20;
    }

    public void setMa20(BigDecimal ma20) {
        this.ma20 = ma20;
    }

    public BigDecimal getMa50() {
        return ma50;
    }

    public void setMa50(BigDecimal ma50) {
        this.ma50 = ma50;
    }

    public BigDecimal getMa100() {
        return ma100;
    }

    public void setMa100(BigDecimal ma100) {
        this.ma100 = ma100;
    }

    public BigDecimal getMa200() {
        return ma200;
    }

    public void setMa200(BigDecimal ma200) {
        this.ma200 = ma200;
    }

    public BigDecimal getEma12() {
        return ema12;
    }

    public void setEma12(BigDecimal ema12) {
        this.ema12 = ema12;
    }

    public BigDecimal getEma26() {
        return ema26;
    }

    public void setEma26(BigDecimal ema26) {
        this.ema26 = ema26;
    }

    public BigDecimal getBbUpper() {
        return bbUpper;
    }

    public void setBbUpper(BigDecimal bbUpper) {
        this.bbUpper = bbUpper;
    }

    public BigDecimal getBbMiddle() {
        return bbMiddle;
    }

    public void setBbMiddle(BigDecimal bbMiddle) {
        this.bbMiddle = bbMiddle;
    }

    public BigDecimal getBbLower() {
        return bbLower;
    }

    public void setBbLower(BigDecimal bbLower) {
        this.bbLower = bbLower;
    }

    public BigDecimal getVolumeMa() {
        return volumeMa;
    }

    public void setVolumeMa(BigDecimal volumeMa) {
        this.volumeMa = volumeMa;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public Boolean getIsComplete() {
        return isComplete;
    }

    public void setIsComplete(Boolean isComplete) {
        this.isComplete = isComplete;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}