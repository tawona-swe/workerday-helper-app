package com.workdayhelper.app.service;

import com.workdayhelper.app.dto.TaskSuggestion;
import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class TaskSuggesterService {

    private final TaskRepository taskRepository;
    private final LlmClient llmClient;

    public TaskSuggesterService(TaskRepository taskRepository, LlmClient llmClient) {
        this.taskRepository = taskRepository;
        this.llmClient = llmClient;
    }

    public List<TaskSuggestion> getDailySuggestions(User user, LocalTime localTime) {
        List<Task> pendingTasks = taskRepository.findByUserAndCompleted(user, false);
        LocalDateTime now = LocalDateTime.now();

        // Score each task
        List<ScoredTask> scoredTasks = new ArrayList<>();
        for (Task task : pendingTasks) {
            double score = computeScore(task, now);
            scoredTasks.add(new ScoredTask(task, score));
        }

        // Sort by score descending
        scoredTasks.sort(Comparator.comparingDouble(ScoredTask::score).reversed());

        // Assign time blocks
        LocalTime highCursor = LocalTime.of(8, 0);
        LocalTime mediumCursor = LocalTime.of(10, 0);
        LocalTime lowCursor = LocalTime.of(14, 0);

        List<TaskSuggestion> suggestions = new ArrayList<>();
        for (ScoredTask st : scoredTasks) {
            Task task = st.task();
            String priority = task.getPriority();
            int duration = task.getEstimatedDurationMinutes() > 0 ? task.getEstimatedDurationMinutes() : 30;

            LocalTime start;
            LocalTime end;

            if ("HIGH".equalsIgnoreCase(priority)) {
                start = highCursor;
                end = start.plusMinutes(duration);
                highCursor = end;
            } else if ("LOW".equalsIgnoreCase(priority)) {
                start = lowCursor;
                end = start.plusMinutes(duration);
                lowCursor = end;
            } else {
                // MEDIUM or other
                start = mediumCursor;
                end = start.plusMinutes(duration);
                mediumCursor = end;
            }

            String rationale = buildRationale(task, st.score(), now);
            String aiAdvice = generateAdvice(task);
            suggestions.add(new TaskSuggestion(
                    task.getId(),
                    task.getTitle(),
                    priority,
                    start,
                    end,
                    st.score(),
                    rationale,
                    aiAdvice
            ));
        }

        return suggestions;
    }

    private double computeScore(Task task, LocalDateTime now) {
        double priorityScore = priorityScore(task.getPriority());
        double dueDateScore = dueDateScore(task.getDueDate(), now);
        double durationScore = durationScore(task.getEstimatedDurationMinutes());

        double total = priorityScore + dueDateScore + durationScore;

        // Override: due within 24h → boost to 100.0
        if (task.getDueDate() != null && task.getDueDate().isBefore(now.plusHours(24))) {
            return 100.0;
        }

        return total;
    }

    private double priorityScore(String priority) {
        if ("HIGH".equalsIgnoreCase(priority)) return 3.0;
        if ("MEDIUM".equalsIgnoreCase(priority)) return 2.0;
        return 1.0; // LOW, null, or other
    }

    private double dueDateScore(java.time.LocalDateTime dueDate, LocalDateTime now) {
        if (dueDate == null) return 0.0;
        if (dueDate.isBefore(now.plusHours(24))) return 10.0;
        if (dueDate.isBefore(now.plusDays(7))) return 2.0;
        if (dueDate.isBefore(now.plusDays(30))) return 1.0;
        return 0.0;
    }

    private double durationScore(int estimatedDurationMinutes) {
        return Math.max(0.0, (60.0 - estimatedDurationMinutes) / 60.0);
    }

    private String buildRationale(Task task, double score, LocalDateTime now) {
        if (score >= 100.0) {
            return "Due within 24 hours — urgent priority.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Priority: ").append(task.getPriority() != null ? task.getPriority() : "NONE");
        if (task.getDueDate() != null) {
            if (task.getDueDate().isBefore(now.plusDays(7))) {
                sb.append(", due within 7 days");
            } else if (task.getDueDate().isBefore(now.plusDays(30))) {
                sb.append(", due within 30 days");
            }
        }
        if (task.getEstimatedDurationMinutes() < 60) {
            sb.append(", short task");
        }
        return sb.toString();
    }

    private String generateAdvice(Task task) {
        try {
            String system = "You are a productivity coach. Give a single concise tip (2-3 sentences max) on how to best tackle the given task. Be practical and specific.";
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "user", "content", "Task: " + task.getTitle()
                            + (task.getPriority() != null ? " (Priority: " + task.getPriority() + ")" : ""))
            );
            return llmClient.chat(system, messages);
        } catch (Exception e) {
            return null;
        }
    }

    private record ScoredTask(Task task, double score) {}
}
