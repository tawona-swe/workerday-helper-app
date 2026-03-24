package com.workdayhelper.app.repository;

import com.workdayhelper.app.model.HealthEvent;
import com.workdayhelper.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HealthEventRepository extends JpaRepository<HealthEvent, Long> {
    List<HealthEvent> findByUserAndEmittedAtBetween(User user, LocalDateTime start, LocalDateTime end);
}
