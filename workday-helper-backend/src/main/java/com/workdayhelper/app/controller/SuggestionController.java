package com.workdayhelper.app.controller;

import com.workdayhelper.app.dto.ContextSuggestion;
import com.workdayhelper.app.dto.TaskSuggestion;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.ContextEngineService;
import com.workdayhelper.app.service.TaskSuggesterService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    private final TaskSuggesterService taskSuggesterService;
    private final ContextEngineService contextEngineService;
    private final UserRepository userRepository;

    public SuggestionController(TaskSuggesterService taskSuggesterService,
                                ContextEngineService contextEngineService,
                                UserRepository userRepository) {
        this.taskSuggesterService = taskSuggesterService;
        this.contextEngineService = contextEngineService;
        this.userRepository = userRepository;
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/daily")
    public ResponseEntity<List<TaskSuggestion>> getDailySuggestions(@AuthenticationPrincipal UserDetails principal) {
        List<TaskSuggestion> suggestions = taskSuggesterService.getDailySuggestions(resolveUser(principal), LocalTime.now());
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/context")
    public ResponseEntity<List<ContextSuggestion>> getContextSuggestions(@AuthenticationPrincipal UserDetails principal) {
        List<ContextSuggestion> suggestions = contextEngineService.getSuggestions(resolveUser(principal), LocalTime.now());
        return ResponseEntity.ok(suggestions);
    }
}
