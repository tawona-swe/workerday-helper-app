package com.workdayhelper.app.controller;

import com.workdayhelper.app.model.ChatMessage;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.AssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;
    private final UserRepository userRepository;

    public AssistantController(AssistantService assistantService, UserRepository userRepository) {
        this.assistantService = assistantService;
        this.userRepository = userRepository;
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> body,
                                         @AuthenticationPrincipal UserDetails principal) {
        String message = body.get("message");
        if (message != null && message.length() > 2000) {
            return ResponseEntity.badRequest().body("Message exceeds 2000 characters");
        }
        try {
            ChatMessage response = assistantService.sendMessage(resolveUser(principal), message);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessage>> getHistory(@AuthenticationPrincipal UserDetails principal) {
        List<ChatMessage> history = assistantService.getHistory(resolveUser(principal));
        return ResponseEntity.ok(history);
    }
}
