package com.workdayhelper.app.service;

import com.workdayhelper.app.model.Reminder;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.ReminderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderService {

    private final ReminderRepository repo;

    public ReminderService(ReminderRepository repo) { this.repo = repo; }

    public List<Reminder> getAll(User user) {
        return repo.findByUser(user);
    }

    public List<Reminder> getActive(User user) {
        return repo.findByUserAndActive(user, true);
    }

    public Reminder getById(Long id, User user) {
        return repo.findByIdAndUser(id, user)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reminder not found"));
    }

    public Reminder create(Reminder reminder, User user) {
        reminder.setUser(user);
        return repo.save(reminder);
    }

    public Reminder update(Long id, Reminder updated, User user) {
        Reminder reminder = getById(id, user);
        reminder.setMessage(updated.getMessage());
        reminder.setType(updated.getType());
        reminder.setIntervalMinutes(updated.getIntervalMinutes());
        reminder.setActive(updated.isActive());
        return repo.save(reminder);
    }

    public Reminder trigger(Long id, User user) {
        Reminder reminder = getById(id, user);
        reminder.setLastTriggered(LocalDateTime.now());
        return repo.save(reminder);
    }

    public void delete(Long id, User user) {
        Reminder reminder = getById(id, user);
        repo.delete(reminder);
    }
}
