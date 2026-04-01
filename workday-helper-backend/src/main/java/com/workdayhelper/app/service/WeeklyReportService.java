package com.workdayhelper.app.service;

import com.workdayhelper.app.dto.DailyAnalytics;
import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.FocusSessionRepository;
import com.workdayhelper.app.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeeklyReportService {

    private final AnalyticsEngineService analyticsEngineService;
    private final TaskRepository taskRepository;
    private final FocusSessionRepository focusSessionRepository;
    private final LlmClient llmClient;

    public WeeklyReportService(AnalyticsEngineService analyticsEngineService,
                                TaskRepository taskRepository,
                                FocusSessionRepository focusSessionRepository,
                                LlmClient llmClient) {
        this.analyticsEngineService = analyticsEngineService;
        this.taskRepository = taskRepository;
        this.focusSessionRepository = focusSessionRepository;
        this.llmClient = llmClient;
    }

    public Map<String, Object> generateReport(User user) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        // Gather daily analytics for the week
        List<DailyAnalytics> dailyList = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            dailyList.add(analyticsEngineService.getDailyAnalytics(user, today.minusDays(i)));
        }

        // Task stats
        List<Task> allTasks = taskRepository.findByUser(user);
        List<Task> weekTasks = allTasks.stream()
            .filter(t -> t.getCreatedAt() != null &&
                !t.getCreatedAt().toLocalDate().isBefore(weekStart))
            .collect(Collectors.toList());
        long completedThisWeek = weekTasks.stream().filter(Task::isCompleted).count();
        long pendingThisWeek = weekTasks.stream().filter(t -> !t.isCompleted()).count();
        long highPriority = weekTasks.stream().filter(t -> "HIGH".equals(t.getPriority())).count();

        // Focus stats
        LocalDateTime weekStartDt = weekStart.atStartOfDay();
        LocalDateTime weekEndDt = today.plusDays(1).atStartOfDay();
        int totalFocusMinutes = focusSessionRepository
            .findByUserAndStartTimeBetween(user, weekStartDt, weekEndDt)
            .stream().filter(s -> s.isCompleted())
            .mapToInt(s -> s.getActualDurationMinutes()).sum();

        int avgProductivity = (int) dailyList.stream()
            .mapToInt(DailyAnalytics::getProductivityScore).average().orElse(0);

        String bestDay = dailyList.stream()
            .max(Comparator.comparingInt(DailyAnalytics::getProductivityScore))
            .map(d -> {
                String name = d.getDate().getDayOfWeek().toString();
                return name.charAt(0) + name.substring(1).toLowerCase();
            })
            .orElse("N/A");

        // Build AI narrative
        String prompt = String.format("""
            Generate a concise weekly productivity report for a user. Be encouraging but honest.
            
            Data:
            - Tasks created this week: %d
            - Tasks completed: %d
            - Tasks still pending: %d
            - High priority tasks: %d
            - Total focus time: %d minutes
            - Average productivity score: %d/100
            - Best day: %s
            - Daily scores: %s
            
            Respond with a JSON object with exactly these fields:
            {
              "summary": "2-3 sentence overview of the week",
              "wentWell": ["up to 3 short bullet points of what went well"],
              "improve": ["up to 3 short bullet points of what to improve"],
              "nextWeekTip": "one actionable tip for next week"
            }
            Respond ONLY with valid JSON, no markdown.
            """,
            weekTasks.size(), completedThisWeek, pendingThisWeek, highPriority,
            totalFocusMinutes, avgProductivity, bestDay,
            dailyList.stream().map(d -> d.getDate().getDayOfWeek().toString().substring(0, 3) + ":" + d.getProductivityScore())
                .collect(Collectors.joining(", "))
        );

        Map<String, Object> aiInsights = Map.of("summary", "", "wentWell", List.of(), "improve", List.of(), "nextWeekTip", "");
        try {
            String response = llmClient.chat("You are a productivity coach.", List.of(Map.of("role", "user", "content", prompt)));
            String json = response.trim().replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
            aiInsights = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception ignored) {}

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("weekStart", weekStart.toString());
        report.put("weekEnd", today.toString());
        report.put("tasksCreated", weekTasks.size());
        report.put("tasksCompleted", completedThisWeek);
        report.put("tasksPending", pendingThisWeek);
        report.put("highPriorityTasks", highPriority);
        report.put("totalFocusMinutes", totalFocusMinutes);
        report.put("avgProductivityScore", avgProductivity);
        report.put("bestDay", bestDay);
        report.put("dailyScores", dailyList.stream().map(d -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("day", d.getDate().getDayOfWeek().toString().substring(0, 3));
            entry.put("score", d.getProductivityScore());
            entry.put("focusMinutes", d.getFocusMinutes());
            return entry;
        }).collect(Collectors.toList()));
        report.put("ai", aiInsights);
        return report;
    }
}
