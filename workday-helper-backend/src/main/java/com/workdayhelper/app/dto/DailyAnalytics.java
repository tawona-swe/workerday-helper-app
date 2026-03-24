package com.workdayhelper.app.dto;

import java.time.LocalDate;

public class DailyAnalytics {

    private LocalDate date;
    private int productivityScore;
    private int focusMinutes;
    private int distractionMinutes;
    private double healthAdherenceRate;
    private double completionRate;

    public DailyAnalytics(LocalDate date, int productivityScore, int focusMinutes,
                          int distractionMinutes, double healthAdherenceRate, double completionRate) {
        this.date = date;
        this.productivityScore = productivityScore;
        this.focusMinutes = focusMinutes;
        this.distractionMinutes = distractionMinutes;
        this.healthAdherenceRate = healthAdherenceRate;
        this.completionRate = completionRate;
    }

    public LocalDate getDate() { return date; }
    public int getProductivityScore() { return productivityScore; }
    public int getFocusMinutes() { return focusMinutes; }
    public int getDistractionMinutes() { return distractionMinutes; }
    public double getHealthAdherenceRate() { return healthAdherenceRate; }
    public double getCompletionRate() { return completionRate; }
}
