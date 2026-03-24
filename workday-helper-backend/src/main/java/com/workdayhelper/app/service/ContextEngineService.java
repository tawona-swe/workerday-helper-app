package com.workdayhelper.app.service;

import com.workdayhelper.app.dto.ContextSuggestion;
import com.workdayhelper.app.dto.TimeWindow;
import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Service
public class ContextEngineService {

    private final TaskRepository taskRepository;
    private final AnalyticsEngineService analyticsEngineService;

    public ContextEngineService(TaskRepository taskRepository,
                                AnalyticsEngineService analyticsEngineService) {
        this.taskRepository = taskRepository;
        this.analyticsEngineService = analyticsEngineService;
    }

    public List<ContextSuggestion> getSuggestions(User user, LocalTime localTime) {
        List<Task> pendingTasks = taskRepository.findByUserAndCompleted(user, false);
        Optional<TimeWindow> peakWindow = analyticsEngineService.getPeakWindow(user);

        List<ContextSuggestion> suggestions = new ArrayList<>();
        Set<Long> addedTaskIds = new HashSet<>();

        int hour = localTime.getHour();

        // 1. Peak window: HIGH priority tasks first
        if (peakWindow.isPresent()) {
            TimeWindow tw = peakWindow.get();
            if (hour >= tw.getStartHour() && hour < tw.getEndHour()) {
                for (Task task : pendingTasks) {
                    if (suggestions.size() >= 5) break;
                    if ("HIGH".equalsIgnoreCase(task.getPriority())) {
                        suggestions.add(new ContextSuggestion(task.getId(), task.getTitle(),
                                "Peak productivity window — tackle your most demanding tasks now."));
                        addedTaskIds.add(task.getId());
                    }
                }
            }
        }

        // 2. Default heuristics
        if (hour < 12) {
            for (Task task : pendingTasks) {
                if (suggestions.size() >= 5) break;
                if (!addedTaskIds.contains(task.getId()) && "HIGH".equalsIgnoreCase(task.getPriority())) {
                    suggestions.add(new ContextSuggestion(task.getId(), task.getTitle(),
                            "Morning focus time — great for high-priority work."));
                    addedTaskIds.add(task.getId());
                }
            }
        } else if (hour >= 14 && hour < 16) {
            for (Task task : pendingTasks) {
                if (suggestions.size() >= 5) break;
                if (!addedTaskIds.contains(task.getId()) && "LOW".equalsIgnoreCase(task.getPriority())) {
                    suggestions.add(new ContextSuggestion(task.getId(), task.getTitle(),
                            "Afternoon — good time for lighter tasks."));
                    addedTaskIds.add(task.getId());
                }
            }
        }

        // 3. Wrap-up suggestion if time >= 16:00
        if (hour >= 16 && suggestions.size() < 5) {
            suggestions.add(new ContextSuggestion(null, "Wrap-up",
                    "End of day — review completed tasks and plan for tomorrow."));
        }

        // 4. Fill remaining slots with other pending tasks
        for (Task task : pendingTasks) {
            if (suggestions.size() >= 5) break;
            if (!addedTaskIds.contains(task.getId())) {
                suggestions.add(new ContextSuggestion(task.getId(), task.getTitle(), "Pending task."));
                addedTaskIds.add(task.getId());
            }
        }

        return suggestions;
    }
}
