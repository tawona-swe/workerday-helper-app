package com.workdayhelper.app.controller;

import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.UserRepository;
import com.workdayhelper.app.service.TaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    public TaskController(TaskService taskService, UserRepository userRepository) {
        this.taskService = taskService;
        this.userRepository = userRepository;
    }

    private User resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<Task> getAll(@AuthenticationPrincipal UserDetails principal) {
        return taskService.getAll(resolveUser(principal));
    }

    @GetMapping("/pending")
    public List<Task> getPending(@AuthenticationPrincipal UserDetails principal) {
        return taskService.getByCompleted(resolveUser(principal), false);
    }

    @GetMapping("/completed")
    public List<Task> getCompleted(@AuthenticationPrincipal UserDetails principal) {
        return taskService.getByCompleted(resolveUser(principal), true);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getById(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(taskService.getById(id, resolveUser(principal)));
    }

    @PostMapping
    public ResponseEntity<Task> create(@RequestBody Task task, @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(taskService.create(task, resolveUser(principal)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> update(@PathVariable Long id, @RequestBody Task task,
                                       @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(taskService.update(id, task, resolveUser(principal)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails principal) {
        taskService.delete(id, resolveUser(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/analytics/completion-rate")
    public ResponseEntity<Map<String, Object>> analytics(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(taskService.analytics(resolveUser(principal)));
    }
}
