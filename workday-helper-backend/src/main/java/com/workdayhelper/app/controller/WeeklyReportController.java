package com.workdayhelper.app.controller;

import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.WeeklyReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;
    private final UserRepository userRepository;

    public WeeklyReportController(WeeklyReportService weeklyReportService, UserRepository userRepository) {
        this.weeklyReportService = weeklyReportService;
        this.userRepository = userRepository;
    }

    @GetMapping("/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklyReport(@AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        return ResponseEntity.ok(weeklyReportService.generateReport(user));
    }
}
