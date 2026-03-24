package com.workdayhelper.app.dto;

public class TimeWindow {

    private int startHour;
    private int endHour;

    public TimeWindow(int startHour, int endHour) {
        this.startHour = startHour;
        this.endHour = endHour;
    }

    public int getStartHour() { return startHour; }
    public int getEndHour() { return endHour; }
}
