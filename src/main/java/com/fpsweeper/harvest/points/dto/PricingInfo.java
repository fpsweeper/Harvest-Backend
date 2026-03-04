package com.fpsweeper.harvest.points.dto;

import java.math.BigDecimal;
import java.util.List;

public class PricingInfo {

    private BigDecimal conversionRate;  // USD to points (e.g., 0.5 means $1 = 0.5 points)
    private List<Package> packages;

    // Constructors
    public PricingInfo() {}

    public PricingInfo(BigDecimal conversionRate, List<Package> packages) {
        this.conversionRate = conversionRate;
        this.packages = packages;
    }

    // Getters and Setters
    public BigDecimal getConversionRate() { return conversionRate; }
    public void setConversionRate(BigDecimal conversionRate) {
        this.conversionRate = conversionRate;
    }

    public List<Package> getPackages() { return packages; }
    public void setPackages(List<Package> packages) { this.packages = packages; }

    // Inner class for packages
    public static class Package {
        private String name;
        private BigDecimal priceUsd;
        private BigDecimal points;
        private BigDecimal bonusPoints;

        public Package() {}

        public Package(String name, BigDecimal priceUsd, BigDecimal points, BigDecimal bonusPoints) {
            this.name = name;
            this.priceUsd = priceUsd;
            this.points = points;
            this.bonusPoints = bonusPoints;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public BigDecimal getPriceUsd() { return priceUsd; }
        public void setPriceUsd(BigDecimal priceUsd) { this.priceUsd = priceUsd; }

        public BigDecimal getPoints() { return points; }
        public void setPoints(BigDecimal points) { this.points = points; }

        public BigDecimal getBonusPoints() { return bonusPoints; }
        public void setBonusPoints(BigDecimal bonusPoints) {
            this.bonusPoints = bonusPoints;
        }
    }
}