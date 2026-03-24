package com.workdayhelper.app.service;

import com.workdayhelper.app.model.FocusSession;
import com.workdayhelper.app.model.HealthEvent;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.HealthEventRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HealthMonitorService {

    private static final String TYPE_BREAK_90MIN = "BREAK_90MIN";
    private static final String TYPE_ACTIVITY_60MIN = "ACTIVITY_60MIN";
    private static final long SUPPRESSION_MINUTES = 15;
    private static final long BREAK_THRESHOLD_MINUTES = 90;
    private static final long ACTIVITY_THRESHOLD_MINUTES = 60;

    private final HealthEventRepository healthEventRepository;
    private final FocusEngineService focusEngineService;
    private final SseEmitterRegistry sseEmitterRegistry;

    // keyed by user ID → last activity time
    private final ConcurrentHashMap<Long, LocalDateTime> lastActivityMap = new ConcurrentHashMap<>();

    // keyed by "userId:type" → dismissal time
    private final ConcurrentHashMap<String, LocalDateTime> dismissalMap = new ConcurrentHashMap<>();

    // keyed by user ID → User object (needed for scheduled check)
    private final ConcurrentHashMap<Long, User> trackedUsers = new ConcurrentHashMap<>();

    public HealthMonitorService(HealthEventRepository healthEventRepository,
                                FocusEngineService focusEngineService,
                                @Lazy SseEmitterRegistry sseEmitterRegistry) {
        this.healthEventRepository = healthEventRepository;
        this.focusEngineService = focusEngineService;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    public void recordActivity(User user) {
        lastActivityMap.put(user.getId(), LocalDateTime.now());
        trackedUsers.put(user.getId(), user);
    }

    public void dismissReminder(User user, String type) {
        String key = user.getId() + ":" + type;
        dismissalMap.put(key, LocalDateTime.now());

        // Mark the most recent non-dismissed event of this type today as dismissed
        LocalDate today = LocalDate.now();
        List<HealthEvent> events = healthEventRepository.findByUserAndEmittedAtBetween(
                user,
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        events.stream()
                .filter(e -> type.equals(e.getType()) && !e.isDismissed())
                .max((a, b) -> a.getEmittedAt().compareTo(b.getEmittedAt()))
                .ifPresent(event -> {
                    event.setDismissed(true);
                    event.setDismissedAt(LocalDateTime.now());
                    healthEventRepository.save(event);
                });
    }

    public List<HealthEvent> getHealthEvents(User user, LocalDate date) {
        return healthEventRepository.findByUserAndEmittedAtBetween(
                user,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        );
    }

    @Scheduled(fixedDelay = 60000)
    public void checkAndEmitReminders() {
        LocalDateTime now = LocalDateTime.now();

        for (Long userId : lastActivityMap.keySet()) {
            LocalDateTime lastActivity = lastActivityMap.get(userId);
            User user = trackedUsers.get(userId);

            if (lastActivity == null || user == null) {
                continue;
            }

            Optional<FocusSession> activeSession = focusEngineService.getActiveSession(user);

            if (activeSession.isPresent()) {
                FocusSession session = activeSession.get();
                long minutesSinceStart = ChronoUnit.MINUTES.between(session.getStartTime(), now);
                if (minutesSinceStart > BREAK_THRESHOLD_MINUTES) {
                    if (!isSuppressed(userId, TYPE_BREAK_90MIN, now)) {
                        emitEvent(user, TYPE_BREAK_90MIN, now);
                    }
                }
            } else {
                long minutesSinceActivity = ChronoUnit.MINUTES.between(lastActivity, now);
                if (minutesSinceActivity > ACTIVITY_THRESHOLD_MINUTES) {
                    if (!isSuppressed(userId, TYPE_ACTIVITY_60MIN, now)) {
                        emitEvent(user, TYPE_ACTIVITY_60MIN, now);
                    }
                }
            }
        }
    }

    private boolean isSuppressed(Long userId, String type, LocalDateTime now) {
        String key = userId + ":" + type;
        LocalDateTime dismissedAt = dismissalMap.get(key);
        if (dismissedAt == null) {
            return false;
        }
        return ChronoUnit.MINUTES.between(dismissedAt, now) < SUPPRESSION_MINUTES;
    }

    private void emitEvent(User user, String type, LocalDateTime now) {
        HealthEvent event = new HealthEvent();
        event.setType(type);
        event.setEmittedAt(now);
        event.setDismissed(false);
        event.setUser(user);
        healthEventRepository.save(event);

        sseEmitterRegistry.sendToUser(user.getId(), "health-reminder",
                Map.of("type", type, "message", getMessageForType(type)));
    }

    private String getMessageForType(String type) {
        return switch (type) {
            case TYPE_BREAK_90MIN -> "You've been focused for 90 minutes — take a 5-minute break.";
            case TYPE_ACTIVITY_60MIN -> "You've been active for 60 minutes — consider a short break.";
            default -> type;
        };
    }
}
