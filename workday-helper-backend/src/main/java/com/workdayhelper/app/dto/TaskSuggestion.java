package com.workdayhelper.app.dto;

import java.time.LocalTime;

public class TaskSuggestion {

    private Long taskId;
    private String title;
    private String priority;
    private LocalTime suggestedStartTime;
    private LocalTime suggestedEndTime;
    private double score;
    private String rationale;
    private String aiAdvice;

    public TaskSuggestion(Long taskId, String title, String priority,
                          LocalTime suggestedStartTime, LocalTime suggestedEndTime,
                          double score, String rationale, String aiAdvice) {
        this.taskId = taskId;
        this.title = title;
        this.priority = priority;
        this.suggestedStartTime = suggestedStartTime;
        this.suggestedEndTime = suggestedEndTime;
        this.score = score;
        this.rationale = rationale;
        this.aiAdvice = aiAdvice;
    }

    public Long getTaskId() { return taskId; }
    public String getTitle() { return title; }
    public String getPriority() { return priority; }
    public LocalTime getSuggestedStartTime() { return suggestedStartTime; }
    public LocalTime getSuggestedEndTime() { return suggestedEndTime; }
    public double getScore() { return score; }
    public String getRationale() { return rationale; }
    public String getAiAdvice() { return aiAdvice; }
}
