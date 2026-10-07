package com.gatelms.controller

import com.gatelms.dto.AchievementDto
import com.gatelms.dto.EngagementSummaryResponse
import com.gatelms.dto.UpdateUserGoalRequest
import com.gatelms.dto.UserGoalDto
import com.gatelms.service.AchievementService
import com.gatelms.service.EngagementService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/engagement")
class EngagementController(
    private val engagementService: EngagementService,
    private val achievementService: AchievementService
) {

    @GetMapping("/summary")
    fun getSummary(): EngagementSummaryResponse {
        return engagementService.getSummary()
    }

    @GetMapping("/achievements")
    fun getAchievements(): List<AchievementDto> {
        return achievementService.getAllAchievements()
    }

    @GetMapping("/goal")
    fun getGoal(): UserGoalDto {
        return engagementService.getUserGoal()
    }

    @PatchMapping("/goal")
    fun updateGoal(@RequestBody req: UpdateUserGoalRequest): UserGoalDto {
        return engagementService.updateUserGoal(req)
    }
}
