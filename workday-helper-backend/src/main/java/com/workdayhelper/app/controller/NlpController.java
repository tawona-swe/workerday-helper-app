package com.workdayhelper.app.controller;

import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.NlpParserService;
import com.workdayhelper.app.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/nlp")
public class NlpController {

    private final NlpParserService nlpParserService;
    private final TaskService taskService;
    private final UserRepository userRepository;

    public NlpController(NlpParserService nlpParserService, TaskService taskService, UserRepository userRepository) {
        this.nlpParserService = nlpParserService;
        this.taskService = taskService;
        this.userRepository = userRepository;
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/parse")
    public ResponseEntity<?> parse(@RequestBody Map<String, String> body,
                                   @AuthenticationPrincipal UserDetails principal) {
        String text = body.get("text");
        String localDateStr = body.get("localDate");

        LocalDate localDate = (localDateStr != null && !localDateStr.isBlank())
                ? LocalDate.parse(localDateStr)
                : LocalDate.now();

        Optional<Task> result = nlpParserService.parse(text, localDate);

        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body("Could not extract a task title from the input");
        }

        User user = resolveUser(principal);
        Task task = result.get();
        Task saved = taskService.create(task, user);
        return ResponseEntity.ok(saved);
    }
}
