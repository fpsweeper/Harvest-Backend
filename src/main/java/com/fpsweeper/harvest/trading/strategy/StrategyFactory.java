package com.fpsweeper.harvest.trading.strategy;

import com.fpsweeper.harvest.trading.StrategyType;
import com.fpsweeper.harvest.trading.TradingBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(StrategyFactory.class);

    @Autowired
    private DCAStrategy dcaStrategy;

    @Autowired
    private GridStrategy gridStrategy;

    @Autowired
    private ScalpingStrategy scalpingStrategy;

    /**
     * Get the appropriate strategy for a bot
     */
    public TradingStrategy getStrategy(TradingBot bot) {
        StrategyType strategyType = bot.getStrategyType();



        switch (strategyType) {
            case DCA:
                return dcaStrategy;

            case GRID:
                return gridStrategy;

            case SCALPING:
                return scalpingStrategy;

            default:

                return dcaStrategy; // Fallback
        }
    }
}