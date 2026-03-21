package com.fpsweeper.harvest.trading;

public class StrategyPointsCost {
    public static final java.math.BigDecimal DCA      = new java.math.BigDecimal("1.0");
    public static final java.math.BigDecimal GRID     = new java.math.BigDecimal("1.0");
    public static final java.math.BigDecimal SCALPING = new java.math.BigDecimal("1.0");

    public static java.math.BigDecimal forStrategy(StrategyType type) {
        switch (type) {
            case GRID:     return GRID;
            case SCALPING: return SCALPING;
            default:       return DCA;
        }
    }
}