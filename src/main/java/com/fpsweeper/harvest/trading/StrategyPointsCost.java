package com.fpsweeper.harvest.trading;

import java.math.BigDecimal;

/**
 * All strategies cost exactly 1 point per successful trade.
 * Keeping this as a utility class so the cost can be changed in one place if needed.
 */
public class StrategyPointsCost {

    public static final BigDecimal POINTS_PER_TRADE = BigDecimal.ONE;

    /** Returns 1 point for any strategy type. */
    public static BigDecimal forStrategy(StrategyType strategyType) {
        return POINTS_PER_TRADE;
    }

    private StrategyPointsCost() {}
}