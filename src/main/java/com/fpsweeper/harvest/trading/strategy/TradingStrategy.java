package com.fpsweeper.harvest.trading.strategy;

import com.fpsweeper.harvest.trading.TradingBot;

public interface TradingStrategy {

    /**
     * Evaluate strategy and return trade signal
     */
    TradeSignal evaluate(TradingBot bot);

    /**
     * Get strategy name
     */
    String getStrategyName();
}