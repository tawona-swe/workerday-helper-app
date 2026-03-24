package com.workdayhelper.app.dto;

import com.workdayhelper.app.model.FocusSession;

import java.util.List;

public class FocusSummary {

    private int totalDeepWorkMinutes;
    private int completedSessionCount;
    private List<FocusSession> sessions;

    public FocusSummary(int totalDeepWorkMinutes, int completedSessionCount, List<FocusSession> sessions) {
        this.totalDeepWorkMinutes = totalDeepWorkMinutes;
        this.completedSessionCount = completedSessionCount;
        this.sessions = sessions;
    }

    public int getTotalDeepWorkMinutes() { return totalDeepWorkMinutes; }
    public int getCompletedSessionCount() { return completedSessionCount; }
    public List<FocusSession> getSessions() { return sessions; }
}
