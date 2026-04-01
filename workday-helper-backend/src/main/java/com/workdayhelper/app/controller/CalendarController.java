package com.workdayhelper.app.controller;

import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.TaskRepository;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.CalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public CalendarController(CalendarService calendarService, UserRepository userRepository, TaskRepository taskRepository) {
        this.calendarService = calendarService;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername()).orElseThrow();
    }

    @GetMapping("/auth-url")
    public ResponseEntity<Map<String, String>> getAuthUrl(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        String url = calendarService.getAuthUrl(user.getId());
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
        try {
            calendarService.handleCallback(code, Long.parseLong(state));
            // Redirect to frontend account page after successful connection
            return ResponseEntity.status(302)
                .header("Location", "https://workerdayapp.tawonarwatida.co.zw/account?calendar=connected")
                .build();
        } catch (Exception e) {
            return ResponseEntity.status(302)
                .header("Location", "https://workerdayapp.tawonarwatida.co.zw/account?calendar=error")
                .build();
        }
    }

    @GetMapping("/events")
    public ResponseEntity<List<Map<String, Object>>> getEvents(@AuthenticationPrincipal UserDetails principal) {
        try {
            User user = resolveUser(principal);
            if (!Boolean.TRUE.equals(user.getGoogleCalendarConnected())) {
                return ResponseEntity.ok(List.of());
            }
            return ResponseEntity.ok(calendarService.getUpcomingEvents(user));
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping("/sync-task/{taskId}")
    public ResponseEntity<Map<String, String>> syncTask(@PathVariable Long taskId,
                                                         @AuthenticationPrincipal UserDetails principal) {
        try {
            User user = resolveUser(principal);
            if (!Boolean.TRUE.equals(user.getGoogleCalendarConnected())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Google Calendar not connected"));
            }
            Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
            String eventId = calendarService.syncTask(user, task);
            return ResponseEntity.ok(Map.of("eventId", eventId, "status", "synced"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Sync failed"));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        return ResponseEntity.ok(Map.of(
            "connected", Boolean.TRUE.equals(user.getGoogleCalendarConnected())
        ));
    }

    @DeleteMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        user.setGoogleAccessToken(null);
        user.setGoogleRefreshToken(null);
        user.setGoogleCalendarConnected(false);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }
}
