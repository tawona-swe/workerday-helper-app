package com.workdayhelper.app.repository;

import com.workdayhelper.app.model.GamificationProfile;
import com.workdayhelper.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GamificationProfileRepository extends JpaRepository<GamificationProfile, Long> {
    Optional<GamificationProfile> findByUser(User user);
}
