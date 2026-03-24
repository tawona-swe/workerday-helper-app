package com.workdayhelper.app.controller;

import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.SseEmitterRegistry;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final UserRepository userRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    public NotificationController(UserRepository userRepository, SseEmitterRegistry sseEmitterRegistry) {
        this.userRepository = userRepository;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/stream")
    public SseEmitter stream(@AuthenticationPrincipal UserDetails principal) {
        User user = resolveUser(principal);
        SseEmitter emitter = new SseEmitter(0L);
        sseEmitterRegistry.register(user.getId(), emitter);
        return emitter;
    }
}
