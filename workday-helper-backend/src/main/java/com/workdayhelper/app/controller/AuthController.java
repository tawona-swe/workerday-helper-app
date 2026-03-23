package com.workdayhelper.app.controller;

import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.security.JwtUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtUtils jwtUtils, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
    }

    record RegisterRequest(@NotBlank String name, @Email @NotBlank String email, @NotBlank String password) {}
    record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        userRepository.save(user);
        String token = jwtUtils.generateToken(user.getEmail());
        return ResponseEntity.ok(Map.of(
            "token", token,
            "user", Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail())
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
            );
            User user = userRepository.findByEmail(req.email()).orElseThrow();
            String token = jwtUtils.generateToken(req.email());
            return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail())
            ));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "createdAt", user.getCreatedAt()
        ));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody Map<String, String> body, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        if (body.containsKey("name")) user.setName(body.get("name"));
        if (body.containsKey("password") && !body.get("password").isBlank()) {
            user.setPassword(passwordEncoder.encode(body.get("password")));
        }
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("name", user.getName(), "email", user.getEmail()));
    }
}
