package com.workdayhelper.app.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "gamification_profiles")
public class GamificationProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int totalPoints;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastStreakDate;

    @OneToMany(mappedBy = "profile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Achievement> achievements;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public GamificationProfile() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getTotalPoints() { return totalPoints; }
    public void setTotalPoints(int totalPoints) { this.totalPoints = totalPoints; }
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
    public LocalDate getLastStreakDate() { return lastStreakDate; }
    public void setLastStreakDate(LocalDate lastStreakDate) { this.lastStreakDate = lastStreakDate; }
    public List<Achievement> getAchievements() { return achievements; }
    public void setAchievements(List<Achievement> achievements) { this.achievements = achievements; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
