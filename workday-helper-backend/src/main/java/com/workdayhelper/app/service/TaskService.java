package com.workdayhelper.app.service;

import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class TaskService {

    private final TaskRepository repo;

    public TaskService(TaskRepository repo) { this.repo = repo; }

    public List<Task> getAll(User user) {
        return repo.findByUser(user);
    }

    public List<Task> getByCompleted(User user, boolean completed) {
        return repo.findByUserAndCompleted(user, completed);
    }

    public Task getById(Long id, User user) {
        return repo.findByIdAndUser(id, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    public Task create(Task task, User user) {
        task.setUser(user);
        return repo.save(task);
    }

    public Task update(Long id, Task updated, User user) {
        Task task = getById(id, user);
        task.setTitle(updated.getTitle());
        task.setDescription(updated.getDescription());
        task.setCompleted(updated.isCompleted());
        task.setPriority(updated.getPriority());
        task.setDueDate(updated.getDueDate());
        return repo.save(task);
    }

    public void delete(Long id, User user) {
        Task task = getById(id, user);
        repo.delete(task);
    }

    public Map<String, Object> analytics(User user) {
        long total = repo.countByUser(user);
        long completed = repo.countByUserAndCompleted(user, true);
        double rate = total == 0 ? 0 : (double) completed / total * 100;
        return Map.of(
            "totalTasks", total,
            "completedTasks", completed,
            "pendingTasks", total - completed,
            "completionRate", rate
        );
    }
}
