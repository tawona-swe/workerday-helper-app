package com.workdayhelper.app.dto;

import java.util.List;

public class WeeklyAnalytics {

    private List<DailyAnalytics> dailyScores;
    private double weeklyAverage;

    public WeeklyAnalytics(List<DailyAnalytics> dailyScores, double weeklyAverage) {
        this.dailyScores = dailyScores;
        this.weeklyAverage = weeklyAverage;
    }

    public List<DailyAnalytics> getDailyScores() { return dailyScores; }
    public double getWeeklyAverage() { return weeklyAverage; }
}
