package com.fpsweeper.harvest.trading.service;

import com.fpsweeper.harvest.trading.MarketDataCache;
import com.fpsweeper.harvest.trading.MarketDataCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ta4j.core.*;
import org.ta4j.core.indicators.*;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.num.DecimalNum;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IndicatorService {

    private static final Logger log = LoggerFactory.getLogger(IndicatorService.class);

    @Autowired
    private MarketDataCacheRepository marketDataCacheRepository;

    @Autowired
    private MarketDataService marketDataService;

    /**
     * Calculate all indicators for a symbol
     */
    public Map<String, BigDecimal> calculateIndicators(String symbol, String timeframe) {
        log.info("🧮 Calculating indicators for {} ({})", symbol, timeframe);

        // Get historical candles (need at least 200 for accurate indicators)
        List<MarketDataCache> candles = marketDataService.getCandles(symbol, timeframe, 200);

        if (candles.isEmpty() || candles.size() < 50) {
            log.error("❌ Not enough candle data to calculate indicators (need 50+, got {})", candles.size());
            return new HashMap<>();
        }

        // Convert to ta4j BarSeries
        BarSeries series = convertToBarSeries(candles, symbol);

        // Calculate all indicators
        Map<String, BigDecimal> indicators = new HashMap<>();

        // RSI
        indicators.put("RSI_14", calculateRSI(series, 14));
        indicators.put("RSI_7", calculateRSI(series, 7));

        // MACD
        Map<String, BigDecimal> macd = calculateMACD(series);
        indicators.putAll(macd);

        // Moving Averages
        indicators.put("MA_20", calculateSMA(series, 20));
        indicators.put("MA_50", calculateSMA(series, 50));
        indicators.put("MA_100", calculateSMA(series, 100));
        indicators.put("MA_200", calculateSMA(series, 200));

        // EMA
        indicators.put("EMA_12", calculateEMA(series, 12));
        indicators.put("EMA_26", calculateEMA(series, 26));

        // Bollinger Bands
        Map<String, BigDecimal> bb = calculateBollingerBands(series, 20, 2);
        indicators.putAll(bb);

        // Volume
        indicators.put("VOLUME", getCurrentVolume(series));
        indicators.put("VOLUME_MA_20", calculateVolumeMA(series, 20));

        // Current Price
        indicators.put("CLOSE_PRICE", getCurrentClosePrice(series));

        log.info("✅ Calculated {} indicators for {}", indicators.size(), symbol);

        // Update cache with calculated indicators
        updateCacheWithIndicators(candles.get(0), indicators);

        return indicators;
    }

    /**
     * Convert MarketDataCache list to ta4j BarSeries
     */
    private BarSeries convertToBarSeries(List<MarketDataCache> candles, String symbol) {
        BarSeries series = new BaseBarSeriesBuilder().withName(symbol).build();

        // Sort by open time ascending (ta4j expects chronological order)
        candles.sort((a, b) -> a.getOpenTime().compareTo(b.getOpenTime()));

        // Determine bar duration from timeframe
        Duration barDuration = parseTimeframeToDuration(candles.get(0).getTimeframe());

        for (MarketDataCache candle : candles) {
            ZonedDateTime endTime = ZonedDateTime.ofInstant(candle.getCloseTime(), ZoneId.systemDefault());

            BaseBar bar = BaseBar.builder(DecimalNum::valueOf, Number.class)
                    .endTime(endTime)  // ✅ Use endTime instead of timePeriod
                    .timePeriod(barDuration)  // ✅ Add duration
                    .openPrice(candle.getOpenPrice())
                    .highPrice(candle.getHighPrice())
                    .lowPrice(candle.getLowPrice())
                    .closePrice(candle.getClosePrice())
                    .volume(candle.getVolume())
                    .build();

            series.addBar(bar);
        }

        return series;
    }

    /**
     * Parse timeframe string to Duration
     */
    private Duration parseTimeframeToDuration(String timeframe) {
        // Common Binance timeframes: 1m, 5m, 15m, 30m, 1h, 4h, 1d, 1w
        timeframe = timeframe.toLowerCase();

        if (timeframe.endsWith("m")) {
            // Minutes
            int minutes = Integer.parseInt(timeframe.replace("m", ""));
            return Duration.ofMinutes(minutes);
        } else if (timeframe.endsWith("h")) {
            // Hours
            int hours = Integer.parseInt(timeframe.replace("h", ""));
            return Duration.ofHours(hours);
        } else if (timeframe.endsWith("d")) {
            // Days
            int days = Integer.parseInt(timeframe.replace("d", ""));
            return Duration.ofDays(days);
        } else if (timeframe.endsWith("w")) {
            // Weeks
            int weeks = Integer.parseInt(timeframe.replace("w", ""));
            return Duration.ofDays(weeks * 7);
        } else {
            // Default to 1 hour
            log.warn("⚠️ Unknown timeframe format: {}, defaulting to 1 hour", timeframe);
            return Duration.ofHours(1);
        }
    }
    /**
     * Calculate RSI (Relative Strength Index)
     */
    private BigDecimal calculateRSI(BarSeries series, int period) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(closePrice, period);

        int lastIndex = series.getEndIndex();
        return BigDecimal.valueOf(rsi.getValue(lastIndex).doubleValue());
    }

    /**
     * Calculate MACD (Moving Average Convergence Divergence)
     */
    private Map<String, BigDecimal> calculateMACD(BarSeries series) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // MACD = EMA(12) - EMA(26)
        EMAIndicator ema12 = new EMAIndicator(closePrice, 12);
        EMAIndicator ema26 = new EMAIndicator(closePrice, 26);
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);

        // Signal line = EMA(9) of MACD
        EMAIndicator signal = new EMAIndicator(macd, 9);

        int lastIndex = series.getEndIndex();

        double macdValue = macd.getValue(lastIndex).doubleValue();
        double signalValue = signal.getValue(lastIndex).doubleValue();
        double histogram = macdValue - signalValue;

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("MACD", BigDecimal.valueOf(macdValue));
        result.put("MACD_SIGNAL", BigDecimal.valueOf(signalValue));
        result.put("MACD_HISTOGRAM", BigDecimal.valueOf(histogram));

        return result;
    }

    /**
     * Calculate Simple Moving Average
     */
    private BigDecimal calculateSMA(BarSeries series, int period) {
        if (series.getBarCount() < period) {
            return BigDecimal.ZERO;
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator sma = new SMAIndicator(closePrice, period);

        int lastIndex = series.getEndIndex();
        return BigDecimal.valueOf(sma.getValue(lastIndex).doubleValue());
    }

    /**
     * Calculate Exponential Moving Average
     */
    private BigDecimal calculateEMA(BarSeries series, int period) {
        if (series.getBarCount() < period) {
            return BigDecimal.ZERO;
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator ema = new EMAIndicator(closePrice, period);

        int lastIndex = series.getEndIndex();
        return BigDecimal.valueOf(ema.getValue(lastIndex).doubleValue());
    }

    /**
     * Calculate Bollinger Bands
     */
    private Map<String, BigDecimal> calculateBollingerBands(BarSeries series, int period, int multiplier) {
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        BollingerBandsMiddleIndicator middle = new BollingerBandsMiddleIndicator(new SMAIndicator(closePrice, period));
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, period);

        BollingerBandsUpperIndicator upper = new BollingerBandsUpperIndicator(middle, stdDev, DecimalNum.valueOf(multiplier));
        BollingerBandsLowerIndicator lower = new BollingerBandsLowerIndicator(middle, stdDev, DecimalNum.valueOf(multiplier));

        int lastIndex = series.getEndIndex();

        Map<String, BigDecimal> result = new HashMap<>();
        result.put("BB_UPPER", BigDecimal.valueOf(upper.getValue(lastIndex).doubleValue()));
        result.put("BB_MIDDLE", BigDecimal.valueOf(middle.getValue(lastIndex).doubleValue()));
        result.put("BB_LOWER", BigDecimal.valueOf(lower.getValue(lastIndex).doubleValue()));

        return result;
    }

    /**
     * Calculate Volume Moving Average
     */
    private BigDecimal calculateVolumeMA(BarSeries series, int period) {
        if (series.getBarCount() < period) {
            return BigDecimal.ZERO;
        }

        VolumeIndicator volume = new VolumeIndicator(series);
        SMAIndicator volumeMA = new SMAIndicator(volume, period);

        int lastIndex = series.getEndIndex();
        return BigDecimal.valueOf(volumeMA.getValue(lastIndex).doubleValue());
    }

    /**
     * Get current volume
     */
    private BigDecimal getCurrentVolume(BarSeries series) {
        int lastIndex = series.getEndIndex();
        return BigDecimal.valueOf(series.getBar(lastIndex).getVolume().doubleValue());
    }

    /**
     * Get current close price
     */
    private BigDecimal getCurrentClosePrice(BarSeries series) {
        int lastIndex = series.getEndIndex();
        return BigDecimal.valueOf(series.getBar(lastIndex).getClosePrice().doubleValue());
    }

    /**
     * Update cache with calculated indicators
     */
    private void updateCacheWithIndicators(MarketDataCache latestCandle, Map<String, BigDecimal> indicators) {
        try {
            latestCandle.setRsi14(indicators.get("RSI_14"));
            latestCandle.setRsi7(indicators.get("RSI_7"));
            latestCandle.setMacd(indicators.get("MACD"));
            latestCandle.setMacdSignal(indicators.get("MACD_SIGNAL"));
            latestCandle.setMacdHistogram(indicators.get("MACD_HISTOGRAM"));
            latestCandle.setMa20(indicators.get("MA_20"));
            latestCandle.setMa50(indicators.get("MA_50"));
            latestCandle.setMa100(indicators.get("MA_100"));
            latestCandle.setMa200(indicators.get("MA_200"));
            latestCandle.setEma12(indicators.get("EMA_12"));
            latestCandle.setEma26(indicators.get("EMA_26"));
            latestCandle.setBbUpper(indicators.get("BB_UPPER"));
            latestCandle.setBbMiddle(indicators.get("BB_MIDDLE"));
            latestCandle.setBbLower(indicators.get("BB_LOWER"));
            latestCandle.setVolumeMa(indicators.get("VOLUME_MA_20"));
            latestCandle.setUpdatedAt(Instant.now());

            marketDataCacheRepository.save(latestCandle);
            log.debug("📊 Updated cache with calculated indicators");

        } catch (Exception e) {
            log.error("❌ Error updating cache with indicators: {}", e.getMessage());
        }
    }
}