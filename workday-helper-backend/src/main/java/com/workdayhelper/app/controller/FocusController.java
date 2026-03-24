package com.workdayhelper.app.controller;

import com.workdayhelper.app.dto.FocusSummary;
import com.workdayhelper.app.model.FocusSession;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.FocusEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/focus")
public class FocusController {

    private final FocusEngineService focusEngineService;
    private final UserRepository userRepository;

    public FocusController(FocusEngineService focusEngineService, UserRepository userRepository) {
        this.focusEngineService = focusEngineService;
        this.userRepository = userRepository;
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/start")
    public ResponseEntity<FocusSession> startSession(@RequestBody Map<String, Object> body,
                                                     @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        Long taskId = ((Number) body.get("taskId")).longValue();
        Integer durationMinutes = body.get("durationMinutes") != null
                ? ((Number) body.get("durationMinutes")).intValue()
                : null;
        FocusSession session = focusEngineService.startSession(user, taskId, durationMinutes);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/end/{sessionId}")
    public ResponseEntity<FocusSession> endSession(@PathVariable Long sessionId,
                                                   @AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        FocusSession session = focusEngineService.endSession(user, sessionId);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/summary")
    public ResponseEntity<FocusSummary> getDailySummary(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        FocusSummary summary = focusEngineService.getDailySummary(user, LocalDate.now());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/active")
    public ResponseEntity<FocusSession> getActiveSession(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        Optional<FocusSession> active = focusEngineService.getActiveSession(user);
        return active.map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
