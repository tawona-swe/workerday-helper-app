package com.workdayhelper.app.service;

import com.workdayhelper.app.dto.DailyAnalytics;
import com.workdayhelper.app.dto.TimeWindow;
import com.workdayhelper.app.dto.WeeklyAnalytics;
import com.workdayhelper.app.model.FocusSession;
import com.workdayhelper.app.model.HealthEvent;
import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.FocusSessionRepository;
import com.workdayhelper.app.repository.HealthEventRepository;
import com.workdayhelper.app.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnalyticsEngineService {

    private final FocusSessionRepository focusSessionRepository;
    private final TaskRepository taskRepository;
    private final HealthEventRepository healthEventRepository;

    public AnalyticsEngineService(FocusSessionRepository focusSessionRepository,
                                  TaskRepository taskRepository,
                                  HealthEventRepository healthEventRepository) {
        this.focusSessionRepository = focusSessionRepository;
        this.taskRepository = taskRepository;
        this.healthEventRepository = healthEventRepository;
    }

    public DailyAnalytics getDailyAnalytics(User user, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();

        // Focus minutes: sum actualDurationMinutes of completed sessions for that day
        List<FocusSession> sessions = focusSessionRepository.findByUserAndStartTimeBetween(user, startOfDay, startOfNextDay);
        int focusMinutes = sessions.stream()
                .filter(FocusSession::isCompleted)
                .mapToInt(FocusSession::getActualDurationMinutes)
                .sum();

        // Completion rate: tasks created on that date
        List<Task> allTasks = taskRepository.findByUser(user);
        List<Task> dayTasks = allTasks.stream()
                .filter(t -> t.getCreatedAt() != null
                        && t.getCreatedAt().toLocalDate().equals(date))
                .collect(Collectors.toList());
        double completionRate;
        if (dayTasks.isEmpty()) {
            completionRate = 0.0;
        } else {
            long completed = dayTasks.stream().filter(Task::isCompleted).count();
            completionRate = (double) completed / dayTasks.size();
        }

        // Health adherence rate: non-dismissed / total events for that day
        List<HealthEvent> healthEvents = healthEventRepository.findByUserAndEmittedAtBetween(user, startOfDay, startOfNextDay);
        double healthAdherenceRate;
        if (healthEvents.isEmpty()) {
            healthAdherenceRate = 1.0;
        } else {
            long nonDismissed = healthEvents.stream().filter(e -> !e.isDismissed()).count();
            healthAdherenceRate = (double) nonDismissed / healthEvents.size();
        }

        // No data at all
        if (focusMinutes == 0 && dayTasks.isEmpty() && healthEvents.isEmpty()) {
            return new DailyAnalytics(date, 0, 0, 0, 1.0, 0.0);
        }

        // Productivity score
        int productivityScore = (int) Math.round(
                completionRate * 40
                + Math.min(focusMinutes / 240.0, 1.0) * 40
                + healthAdherenceRate * 20
        );
        productivityScore = Math.max(0, Math.min(100, productivityScore));

        int distractionMinutes = Math.max(0, 480 - focusMinutes);

        return new DailyAnalytics(date, productivityScore, focusMinutes, distractionMinutes, healthAdherenceRate, completionRate);
    }

    public WeeklyAnalytics getWeeklyAnalytics(User user) {
        LocalDate today = LocalDate.now();
        List<DailyAnalytics> dailyList = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            dailyList.add(getDailyAnalytics(user, today.minusDays(i)));
        }

        double avg = dailyList.stream()
                .mapToInt(DailyAnalytics::getProductivityScore)
                .average()
                .orElse(0.0);

        double weeklyAverage = BigDecimal.valueOf(avg)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        return new WeeklyAnalytics(dailyList, weeklyAverage);
    }

    public Optional<TimeWindow> getPeakWindow(User user) {
        LocalDateTime rangeStart = LocalDateTime.now().minusYears(10);
        LocalDateTime rangeEnd = LocalDateTime.now().plusYears(1);

        List<FocusSession> allSessions = focusSessionRepository.findByUserAndStartTimeBetween(user, rangeStart, rangeEnd);

        // Need at least 5 distinct calendar days
        Set<LocalDate> distinctDays = allSessions.stream()
                .map(s -> s.getStartTime().toLocalDate())
                .collect(Collectors.toSet());

        if (distinctDays.size() < 5) {
            return Optional.empty();
        }

        // For each startHour 0..22, sum focus minutes of sessions starting in that 2-hour window
        int bestHour = 0;
        int bestMinutes = -1;

        for (int h = 0; h <= 22; h++) {
            final int hour = h;
            int windowMinutes = allSessions.stream()
                    .filter(s -> {
                        int sh = s.getStartTime().getHour();
                        return sh == hour || sh == hour + 1;
                    })
                    .mapToInt(FocusSession::getActualDurationMinutes)
                    .sum();

            if (windowMinutes > bestMinutes) {
                bestMinutes = windowMinutes;
                bestHour = hour;
            }
        }

        return Optional.of(new TimeWindow(bestHour, bestHour + 2));
    }
}
