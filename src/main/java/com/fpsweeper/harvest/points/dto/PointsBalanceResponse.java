package com.fpsweeper.harvest.points.dto;

import java.math.BigDecimal;

public class PointsBalanceResponse {

    private BigDecimal points;

    // Constructors
    public PointsBalanceResponse() {}

    public PointsBalanceResponse(BigDecimal points) {
        this.points = points;
    }

    // Getters and Setters
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }
}