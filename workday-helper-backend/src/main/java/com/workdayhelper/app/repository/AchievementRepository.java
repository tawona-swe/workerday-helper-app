package com.workdayhelper.app.repository;

import com.workdayhelper.app.model.Achievement;
import com.workdayhelper.app.model.GamificationProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByProfile(GamificationProfile profile);
}
