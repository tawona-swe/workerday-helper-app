package com.workdayhelper.app.service;

import com.workdayhelper.app.model.Achievement;
import com.workdayhelper.app.model.GamificationProfile;
import com.workdayhelper.app.model.User;
import com.workdayhelper.app.repository.AchievementRepository;
import com.workdayhelper.app.repository.GamificationProfileRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class GamificationEngineService {

    private final GamificationProfileRepository gamificationProfileRepository;
    private final AchievementRepository achievementRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    public GamificationEngineService(GamificationProfileRepository gamificationProfileRepository,
                                     AchievementRepository achievementRepository,
                                     @Lazy SseEmitterRegistry sseEmitterRegistry) {
        this.gamificationProfileRepository = gamificationProfileRepository;
        this.achievementRepository = achievementRepository;
        this.sseEmitterRegistry = sseEmitterRegistry;
    }

    private GamificationProfile getOrCreateProfile(User user) {
        return gamificationProfileRepository.findByUser(user).orElseGet(() -> {
            GamificationProfile profile = new GamificationProfile();
            profile.setUser(user);
            profile.setTotalPoints(0);
            profile.setCurrentStreak(0);
            profile.setLongestStreak(0);
            profile.setAchievements(new java.util.ArrayList<>());
            return gamificationProfileRepository.save(profile);
        });
    }

    public void onTaskCompleted(User user) {
        GamificationProfile profile = getOrCreateProfile(user);
        profile.setTotalPoints(profile.getTotalPoints() + 10);
        gamificationProfileRepository.save(profile);
    }

    public void onFocusSessionCompleted(User user, int durationMinutes) {
        if (durationMinutes >= 25) {
            GamificationProfile profile = getOrCreateProfile(user);
            profile.setTotalPoints(profile.getTotalPoints() + 20);
            gamificationProfileRepository.save(profile);
        }
    }

    public void onDailyScoreRecorded(User user, int score) {
        GamificationProfile profile = getOrCreateProfile(user);

        if (score >= 60) {
            profile.setCurrentStreak(profile.getCurrentStreak() + 1);
            if (profile.getCurrentStreak() > profile.getLongestStreak()) {
                profile.setLongestStreak(profile.getCurrentStreak());
            }
            profile.setLastStreakDate(LocalDate.now());
        } else {
            profile.setCurrentStreak(0);
        }

        int[] milestones = {5, 10, 30};
        for (int milestone : milestones) {
            if (profile.getCurrentStreak() == milestone) {
                String achievementName = milestone + "-Day Streak";
                List<Achievement> existing = achievementRepository.findByProfile(profile);
                boolean alreadyExists = existing.stream()
                        .anyMatch(a -> achievementName.equals(a.getName()));
                if (!alreadyExists) {
                    Achievement achievement = new Achievement();
                    achievement.setName(achievementName);
                    achievement.setDescription("Maintained a " + milestone + "-day productivity streak");
                    achievement.setUnlockedAt(LocalDateTime.now());
                    achievement.setProfile(profile);
                    achievementRepository.save(achievement);

                    sseEmitterRegistry.sendToUser(user.getId(), "achievement",
                            Map.of("name", achievementName, "message", "Achievement unlocked: " + achievementName));
                }
            }
        }

        gamificationProfileRepository.save(profile);
    }

    public GamificationProfile getProfile(User user) {
        return getOrCreateProfile(user);
    }
}
