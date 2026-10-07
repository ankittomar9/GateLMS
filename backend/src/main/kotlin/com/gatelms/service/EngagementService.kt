package com.gatelms.service

import com.gatelms.dto.*
import com.gatelms.entity.UserGoal
import com.gatelms.error.ValidationException
import com.gatelms.repository.DailyRollupRepository
import com.gatelms.repository.UserGoalRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Service
class EngagementService(
    private val userGoalRepository: UserGoalRepository,
    private val dailyRollupRepository: DailyRollupRepository,
    private val achievementService: AchievementService,
    @Autowired(required = false)
    private val revisionService: RevisionService? = null
) {

    @Transactional
    fun getSummary(): EngagementSummaryResponse {
        val userGoal = getOrCreateGoal()
        val today = LocalDate.now()

        val rollups = dailyRollupRepository.findAllByOrderByActivityDateDesc()
        val activityMap = rollups.associate { it.activityDate to it.questionsAnswered }

        val streakResult = StreakCalculator.computeStreak(
            activityMap = activityMap,
            today = today,
            streakProtection = userGoal.streakProtection
        )

        // Wiretap achievement check for streak
        if (streakResult.streak > 0) {
            achievementService.checkOnStreak(streakResult.streak)
        }

        val todayDone = activityMap[today] ?: 0
        val target = userGoal.dailyTargetQuestions
        val met = todayDone >= target

        val dueOverview = revisionService?.getOverview()
        val dueMistakes = dueOverview?.dueMistakes ?: 0L
        val dueTopics = dueOverview?.dueTopics ?: 0L

        return EngagementSummaryResponse(
            streak = streakResult.streak,
            protectedDate = streakResult.protectedDate,
            streakAtRisk = streakResult.streakAtRisk,
            goal = DailyGoalProgressDto(
                target = target,
                done = todayDone,
                met = met
            ),
            dueToday = DueTodayDto(
                mistakes = dueMistakes,
                topics = dueTopics
            )
        )
    }

    @Transactional(readOnly = true)
    fun getUserGoal(): UserGoalDto {
        return getOrCreateGoal().toDto()
    }

    @Transactional
    fun updateUserGoal(req: UpdateUserGoalRequest): UserGoalDto {
        val goal = getOrCreateGoal()

        req.dailyTargetQuestions?.let { target ->
            if (target < 5 || target > 200) {
                throw ValidationException(
                    "Daily target questions must be between 5 and 200",
                    mapOf("dailyTargetQuestions" to "Must be between 5 and 200")
                )
            }
            goal.dailyTargetQuestions = target
        }

        req.streakProtection?.let {
            goal.streakProtection = it
        }

        goal.updatedAt = Instant.now()
        val saved = userGoalRepository.save(goal)
        return saved.toDto()
    }

    private fun getOrCreateGoal(): UserGoal {
        return userGoalRepository.findFirstByOrderByUpdatedAtDesc() ?: run {
            val newGoal = UserGoal(
                dailyTargetQuestions = 20,
                streakProtection = true,
                updatedAt = Instant.now()
            )
            userGoalRepository.save(newGoal)
        }
    }

    private fun UserGoal.toDto() = UserGoalDto(
        dailyTargetQuestions = dailyTargetQuestions,
        streakProtection = streakProtection,
        updatedAt = updatedAt
    )
}
