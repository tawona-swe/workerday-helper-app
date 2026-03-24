package com.workdayhelper.app.repository;

import com.workdayhelper.app.model.FocusSession;
import com.workdayhelper.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FocusSessionRepository extends JpaRepository<FocusSession, Long> {
    Optional<FocusSession> findByUserAndEndTimeIsNull(User user);
    List<FocusSession> findByUserAndStartTimeBetween(User user, LocalDateTime start, LocalDateTime end);
}
