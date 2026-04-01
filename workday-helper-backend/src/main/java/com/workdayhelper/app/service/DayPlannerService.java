package com.workdayhelper.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DayPlannerService {

    private final LlmClient llmClient;
    private final TaskRepository taskRepository;
    private final CalendarService calendarService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DayPlannerService(LlmClient llmClient, TaskRepository taskRepository, CalendarService calendarService) {
        this.llmClient = llmClient;
        this.taskRepository = taskRepository;
        this.calendarService = calendarService;
    }

    public List<Map<String, Object>> planDay(User user) {
        List<Task> pending = taskRepository.findByUserAndCompleted(user, false);
        if (pending.isEmpty()) return List.of();

        // Get calendar events if connected
        List<Map<String, Object>> calendarEvents = List.of();
        if (Boolean.TRUE.equals(user.getGoogleCalendarConnected())) {
            try {
                calendarEvents = calendarService.getUpcomingEvents(user);
            } catch (Exception ignored) {}
        }

        String taskList = pending.stream().map(t ->
            String.format("- [%s] %s (priority: %s, est: %d min%s)",
                t.getId(), t.getTitle(),
                t.getPriority() != null ? t.getPriority() : "MEDIUM",
                t.getEstimatedDurationMinutes(),
                t.getDueDate() != null ? ", due: " + t.getDueDate().toLocalDate() : "")
        ).collect(Collectors.joining("\n"));

        String busySlots = calendarEvents.isEmpty() ? "No calendar events today." :
            calendarEvents.stream().map(e ->
                String.format("- %s: %s to %s", e.get("title"), e.get("start"), e.get("end"))
            ).collect(Collectors.joining("\n"));

        String systemPrompt = """
            You are an AI day planner. Given a list of tasks and calendar events, create an optimal daily schedule.
            Work hours are 8:00 AM to 6:00 PM. Schedule tasks in free slots, avoiding calendar conflicts.
            Prioritize HIGH priority tasks first, then MEDIUM, then LOW.
            Add short breaks (15 min) after every 90 minutes of work.
            
            Respond ONLY with a valid JSON array. No explanation, no markdown, just the JSON array.
            Each item must have exactly these fields:
            {
              "time": "09:00",
              "endTime": "10:00",
              "title": "Task or break title",
              "type": "task|break|meeting",
              "priority": "HIGH|MEDIUM|LOW|null",
              "taskId": 123 or null,
              "tip": "One short actionable tip for this block"
            }
            """;

        String userMessage = String.format(
            "My tasks:\n%s\n\nMy calendar events today:\n%s\n\nCreate my optimal day plan as a JSON array.",
            taskList, busySlots
        );

        List<Map<String, String>> messages = List.of(Map.of("role", "user", "content", userMessage));

        try {
            String response = llmClient.chat(systemPrompt, messages);
            // Strip markdown code blocks if present
            String json = response.trim()
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate day plan: " + e.getMessage(), e);
        }
    }
}
