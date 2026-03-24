package com.workdayhelper.app.controller;

import com.workdayhelper.app.model.GamificationProfile;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.GamificationEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gamification")
public class GamificationController {

    private final GamificationEngineService gamificationEngineService;
    private final UserRepository userRepository;

    public GamificationController(GamificationEngineService gamificationEngineService,
                                   UserRepository userRepository) {
        this.gamificationEngineService = gamificationEngineService;
        this.userRepository = userRepository;
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/profile")
    public ResponseEntity<GamificationProfile> getProfile(@AuthenticationPrincipal UserDetails principal) {
        GamificationProfile profile = gamificationEngineService.getProfile(resolveUser(principal));
        return ResponseEntity.ok(profile);
    }
}
