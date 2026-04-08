package com.workdayhelper.app.controller;

import com.workdayhelper.app.repository.ReminderRepository;
import com.workdayhelper.app.repository.TaskRepository;
import com.workdayhelper.app.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ReminderRepository reminderRepository;

    public AdminController(UserRepository userRepository, TaskRepository taskRepository, ReminderRepository reminderRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.reminderRepository = reminderRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
            "totalUsers", userRepository.count(),
            "totalTasks", taskRepository.count(),
            "completedTasks", taskRepository.countByCompleted(true),
            "pendingTasks", taskRepository.countByCompleted(false),
            "totalReminders", reminderRepository.count(),
            "activeReminders", reminderRepository.countByActive(true)
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<?> users() {
        return ResponseEntity.ok(
            userRepository.findAll().stream().map(u -> Map.of(
                "id", u.getId(),
                "name", u.getName(),
                "email", u.getEmail(),
                "createdAt", u.getCreatedAt().toString(),
                "calendarConnected", Boolean.TRUE.equals(u.getGoogleCalendarConnected())
            )).toList()
        );
    }
}
