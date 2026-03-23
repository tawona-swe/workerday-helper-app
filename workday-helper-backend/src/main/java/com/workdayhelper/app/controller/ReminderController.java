package com.workdayhelper.app.controller;

import com.workdayhelper.app.model.Reminder;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.ReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final UserRepository userRepository;

    public ReminderController(ReminderService reminderService, UserRepository userRepository) {
        this.reminderService = reminderService;
        this.userRepository = userRepository;
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<Reminder> getAll(@AuthenticationPrincipal UserDetails principal) {
        return reminderService.getAll(resolveUser(principal));
    }

    @GetMapping("/active")
    public List<Reminder> getActive(@AuthenticationPrincipal UserDetails principal) {
        return reminderService.getActive(resolveUser(principal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reminder> getById(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(reminderService.getById(id, resolveUser(principal)));
    }

    @PostMapping
    public ResponseEntity<Reminder> create(@RequestBody Reminder reminder,
                                           @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(reminderService.create(reminder, resolveUser(principal)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reminder> update(@PathVariable Long id, @RequestBody Reminder reminder,
                                           @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(reminderService.update(id, reminder, resolveUser(principal)));
    }

    @PatchMapping("/{id}/trigger")
    public ResponseEntity<Reminder> trigger(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(reminderService.trigger(id, resolveUser(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        reminderService.delete(id, resolveUser(principal));
        return ResponseEntity.noContent().build();
    }
}
