package com.workdayhelper.app.controller;

import com.workdayhelper.app.dto.DailyAnalytics;
import com.workdayhelper.app.dto.TimeWindow;
import com.workdayhelper.app.dto.WeeklyAnalytics;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.AnalyticsEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsEngineService analyticsEngineService;
    private final UserRepository userRepository;

    public AnalyticsController(AnalyticsEngineService analyticsEngineService, UserRepository userRepository) {
        this.analyticsEngineService = analyticsEngineService;
        this.userRepository = userRepository;
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/daily")
    public ResponseEntity<DailyAnalytics> getDaily(
            @RequestParam(required = false) String date,
            @AuthenticationPrincipal UserDetails principal) {
        LocalDate localDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();
        DailyAnalytics analytics = analyticsEngineService.getDailyAnalytics(resolveUser(principal), localDate);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/weekly")
    public ResponseEntity<WeeklyAnalytics> getWeekly(@AuthenticationPrincipal UserDetails principal) {
        WeeklyAnalytics analytics = analyticsEngineService.getWeeklyAnalytics(resolveUser(principal));
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/peak-window")
    public ResponseEntity<TimeWindow> getPeakWindow(@AuthenticationPrincipal UserDetails principal) {
        Optional<TimeWindow> window = analyticsEngineService.getPeakWindow(resolveUser(principal));
        return window.map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
