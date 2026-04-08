package com.workdayhelper.app.repository;

import com.workdayhelper.app.model.Task;
import com.workdayhelper.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUser(User user);
    List<Task> findByUserAndCompleted(User user, boolean completed);
    Optional<Task> findByIdAndUser(Long id, User user);
    long countByUser(User user);
    long countByUserAndCompleted(User user, boolean completed);
    long countByCompleted(boolean completed);
}
