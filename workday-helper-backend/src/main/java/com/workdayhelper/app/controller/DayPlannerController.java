package com.workdayhelper.app.controller;

import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.DayPlannerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planner")
public class DayPlannerController {

    private final DayPlannerService dayPlannerService;
    private final UserRepository userRepository;

    public DayPlannerController(DayPlannerService dayPlannerService, UserRepository userRepository) {
        this.dayPlannerService = dayPlannerService;
        this.userRepository = userRepository;
    }

    @GetMapping("/plan")
    public ResponseEntity<List<Map<String, Object>>> getPlan(@AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        return ResponseEntity.ok(dayPlannerService.planDay(user));
    }
}
