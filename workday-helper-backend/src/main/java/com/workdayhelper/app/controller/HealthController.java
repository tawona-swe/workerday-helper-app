package com.workdayhelper.app.controller;

import com.workdayhelper.app.model.HealthEvent;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.HealthMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthMonitorService healthMonitorService;
    private final UserRepository userRepository;

    public HealthController(HealthMonitorService healthMonitorService, UserRepository userRepository) {
        this.healthMonitorService = healthMonitorService;
        this.userRepository = userRepository;
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/activity")
    public ResponseEntity<Void> recordActivity(@AuthenticationPrincipal UserDetails principal) {
        healthMonitorService.recordActivity(resolveUser(principal));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/dismiss/{type}")
    public ResponseEntity<Void> dismissReminder(@PathVariable String type,
                                                @AuthenticationPrincipal UserDetails principal) {
        healthMonitorService.dismissReminder(resolveUser(principal), type);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/events")
    public ResponseEntity<List<HealthEvent>> getHealthEvents(@AuthenticationPrincipal UserDetails principal) {
        List<HealthEvent> events = healthMonitorService.getHealthEvents(resolveUser(principal), LocalDate.now());
        return ResponseEntity.ok(events);
    }
}
