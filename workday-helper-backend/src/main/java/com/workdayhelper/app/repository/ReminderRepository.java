package com.workdayhelper.app.repository;

import com.workdayhelper.app.model.Reminder;
import com.workdayhelper.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByUser(User user);
    List<Reminder> findByUserAndActive(User user, boolean active);
    Optional<Reminder> findByIdAndUser(Long id, User user);
}
