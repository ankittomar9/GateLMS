package com.gatelms.controller;

import com.gatelms.dto.AchievementDto;
import com.gatelms.dto.EngagementSummaryResponse;
import com.gatelms.dto.UpdateUserGoalRequest;
import com.gatelms.dto.UserGoalDto;
import com.gatelms.service.AchievementService;
import com.gatelms.service.EngagementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/engagement")
public class EngagementController {

    private final EngagementService engagementService;
    private final AchievementService achievementService;

    public EngagementController(
            EngagementService engagementService,
            AchievementService achievementService
    ) {
        this.engagementService = engagementService;
        this.achievementService = achievementService;
    }

    @GetMapping("/summary")
    public EngagementSummaryResponse getSummary() {
        return engagementService.getSummary();
    }

    @GetMapping("/achievements")
    public List<AchievementDto> getAchievements() {
        return achievementService.getAllAchievements();
    }

    @GetMapping("/goal")
    public UserGoalDto getGoal() {
        return engagementService.getUserGoal();
    }

    @PatchMapping("/goal")
    public UserGoalDto updateGoal(@RequestBody UpdateUserGoalRequest req) {
        return engagementService.updateUserGoal(req);
    }
}
