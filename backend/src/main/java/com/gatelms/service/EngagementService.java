package com.gatelms.service;

import com.gatelms.dto.EngagementDtos.*;
import com.gatelms.dto.RevisionDtos.RevisionOverviewDto;
import com.gatelms.entity.DailyRollup;
import com.gatelms.entity.UserGoal;
import com.gatelms.error.Exceptions.ValidationException;
import com.gatelms.repository.DailyRollupRepository;
import com.gatelms.repository.UserGoalRepository;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EngagementService {

    private final UserGoalRepository userGoalRepository;
    private final DailyRollupRepository dailyRollupRepository;
    private final AchievementService achievementService;
    private final CurrentUserProvider currentUserProvider;
    private final RevisionService revisionService;

    public EngagementService(
        UserGoalRepository userGoalRepository,
        DailyRollupRepository dailyRollupRepository,
        AchievementService achievementService,
        CurrentUserProvider currentUserProvider,
        @Autowired(required = false) RevisionService revisionService
    ) {
        this.userGoalRepository = userGoalRepository;
        this.dailyRollupRepository = dailyRollupRepository;
        this.achievementService = achievementService;
        this.currentUserProvider = currentUserProvider;
        this.revisionService = revisionService;
    }

    @Transactional
    public EngagementSummaryResponse getSummary() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        UserGoal userGoal = getOrCreateGoal();
        LocalDate today = LocalDate.now();

        List<DailyRollup> rollups = dailyRollupRepository.findAllByUserIdOrderByActivityDateDesc(currentUserId);
        Map<LocalDate, Integer> activityMap = rollups.stream()
            .collect(Collectors.toMap(DailyRollup::getActivityDate, DailyRollup::getQuestionsAnswered, (a, b) -> a));

        StreakCalculator.StreakResult streakResult = StreakCalculator.computeStreak(
            activityMap,
            today,
            Boolean.TRUE.equals(userGoal.getStreakProtection())
        );

        if (streakResult.streak() > 0) {
            achievementService.checkOnStreak(streakResult.streak(), currentUserId);
        }

        int todayDone = activityMap.getOrDefault(today, 0);
        int target = userGoal.getDailyTargetQuestions();
        boolean met = todayDone >= target;

        RevisionOverviewDto dueOverview = revisionService != null ? revisionService.getOverview() : null;
        long dueMistakes = dueOverview != null ? dueOverview.dueMistakes() : 0L;
        long dueTopics = dueOverview != null ? dueOverview.dueTopics() : 0L;

        return new EngagementSummaryResponse(
            streakResult.streak(),
            streakResult.protectedDate(),
            streakResult.streakAtRisk(),
            new DailyGoalProgressDto(target, todayDone, met),
            new DueTodayDto(dueMistakes, dueTopics)
        );
    }

    @Transactional(readOnly = true)
    public UserGoalDto getUserGoal() {
        return toDto(getOrCreateGoal());
    }

    @Transactional
    public UserGoalDto updateUserGoal(UpdateUserGoalRequest req) {
        UserGoal goal = getOrCreateGoal();

        if (req.dailyTargetQuestions() != null) {
            int target = req.dailyTargetQuestions();
            if (target < 5 || target > 200) {
                throw new ValidationException(
                    "Daily target questions must be between 5 and 200",
                    Map.of("dailyTargetQuestions", "Must be between 5 and 200")
                );
            }
            goal.setDailyTargetQuestions(target);
        }

        if (req.streakProtection() != null) {
            goal.setStreakProtection(req.streakProtection());
        }

        goal.setUpdatedAt(Instant.now());
        UserGoal saved = userGoalRepository.save(goal);
        return toDto(saved);
    }

    private UserGoal getOrCreateGoal() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        UserGoal existing = userGoalRepository.findFirstByUserIdOrderByUpdatedAtDesc(currentUserId);
        if (existing != null) {
            return existing;
        }

        UserGoal newGoal = new UserGoal();
        newGoal.setUserId(currentUserId);
        newGoal.setDailyTargetQuestions(20);
        newGoal.setStreakProtection(true);
        newGoal.setUpdatedAt(Instant.now());
        return userGoalRepository.save(newGoal);
    }

    private UserGoalDto toDto(UserGoal g) {
        return new UserGoalDto(
            g.getDailyTargetQuestions(),
            g.getStreakProtection(),
            g.getUpdatedAt()
        );
    }
}
