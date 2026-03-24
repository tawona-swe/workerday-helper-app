package com.workdayhelper.app.dto;

public class ContextSuggestion {

    private Long taskId;
    private String title;
    private String rationale;

    public ContextSuggestion(Long taskId, String title, String rationale) {
        this.taskId = taskId;
        this.title = title;
        this.rationale = rationale;
    }

    public Long getTaskId() { return taskId; }
    public String getTitle() { return title; }
    public String getRationale() { return rationale; }
}
