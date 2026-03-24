package com.workdayhelper.app.service;

import com.workdayhelper.app.dto.FocusSummary;
import com.workdayhelper.app.exception.ConflictException;
import com.workdayhelper.app.exception.ForbiddenException;
import com.workdayhelper.app.model.FocusSession;
import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.FocusSessionRepository;
import com.workdayhelper.app.repository.TaskRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FocusEngineService {

    private final FocusSessionRepository focusSessionRepository;
    private final TaskRepository taskRepository;
    private final GamificationEngineService gamificationEngineService;

    public FocusEngineService(FocusSessionRepository focusSessionRepository,
                              TaskRepository taskRepository,
                              @Lazy GamificationEngineService gamificationEngineService) {
        this.focusSessionRepository = focusSessionRepository;
        this.taskRepository = taskRepository;
        this.gamificationEngineService = gamificationEngineService;
    }

    public FocusSession startSession(User user, Long taskId, Integer durationMinutes) {
        Optional<FocusSession> existing = focusSessionRepository.findByUserAndEndTimeIsNull(user);
        if (existing.isPresent()) {
            FocusSession activeSession = existing.get();
            throw new ConflictException("Active session already exists: " + activeSession.getId());
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        FocusSession session = new FocusSession();
        session.setStartTime(LocalDateTime.now());
        session.setTargetDurationMinutes(durationMinutes != null ? durationMinutes : 25);
        session.setTask(task);
        session.setUser(user);

        return focusSessionRepository.save(session);
    }

    public FocusSession endSession(User user, Long sessionId) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        if (!session.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Session does not belong to this user");
        }

        session.setEndTime(LocalDateTime.now());
        int actualDurationMinutes = (int) ChronoUnit.MINUTES.between(session.getStartTime(), session.getEndTime());
        session.setActualDurationMinutes(actualDurationMinutes);
        session.setCompleted(true);

        FocusSession saved = focusSessionRepository.save(session);

        if (actualDurationMinutes >= 25) {
            gamificationEngineService.onFocusSessionCompleted(user, actualDurationMinutes);
        }

        return saved;
    }

    public FocusSummary getDailySummary(User user, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<FocusSession> sessions = focusSessionRepository.findByUserAndStartTimeBetween(user, start, end);

        List<FocusSession> completedSessions = sessions.stream()
                .filter(FocusSession::isCompleted)
                .collect(Collectors.toList());

        int totalDeepWorkMinutes = completedSessions.stream()
                .mapToInt(FocusSession::getActualDurationMinutes)
                .sum();

        return new FocusSummary(totalDeepWorkMinutes, completedSessions.size(), completedSessions);
    }

    public Optional<FocusSession> getActiveSession(User user) {
        return focusSessionRepository.findByUserAndEndTimeIsNull(user);
    }
}
