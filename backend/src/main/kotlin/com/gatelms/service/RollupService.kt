package com.gatelms.service

import com.gatelms.dto.DayRollupDto
import com.gatelms.dto.Rolling7Stats
import com.gatelms.dto.TrendsResponse
import com.gatelms.error.ValidationException
import com.gatelms.repository.DailyRollupRepository
import com.gatelms.security.CurrentUserProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

@Service
class RollupService(
    private val dailyRollupRepository: DailyRollupRepository,
    private val currentUserProvider: CurrentUserProvider
) {

    @Transactional
    fun recordAnswer(activityDate: LocalDate, correct: Boolean, seconds: Int, mode: String, userId: UUID? = null) {
        val currentUserId = userId ?: currentUserProvider.getCurrentUserId()
        val isPractice = mode.equals("practice", ignoreCase = true)
        val minutesPracticed = if (isPractice) Math.max(0, (seconds + 59) / 60) else 0
        val minutesMocked = if (!isPractice) Math.max(0, (seconds + 59) / 60) else 0

        dailyRollupRepository.upsertRollup(
            userId = currentUserId,
            activityDate = activityDate,
            questionsAnswered = 1,
            correctAnswers = if (correct) 1 else 0,
            minutesPracticed = minutesPracticed,
            minutesMocked = minutesMocked,
            mockAttempts = 0
        )
    }

    @Transactional
    fun recordMockSubmit(
        activityDate: LocalDate,
        questionsAnswered: Int,
        correctAnswers: Int,
        minutesMocked: Int,
        userId: UUID? = null
    ) {
        val currentUserId = userId ?: currentUserProvider.getCurrentUserId()
        dailyRollupRepository.upsertRollup(
            userId = currentUserId,
            activityDate = activityDate,
            questionsAnswered = questionsAnswered,
            correctAnswers = correctAnswers,
            minutesPracticed = 0,
            minutesMocked = minutesMocked,
            mockAttempts = 1
        )
    }

    @Transactional(readOnly = true)
    fun getTrends(days: Int): TrendsResponse {
        if (days < 7 || days > 365) {
            throw ValidationException(
                "Invalid days parameter",
                mapOf("days" to "Days must be between 7 and 365")
            )
        }

        val currentUserId = currentUserProvider.getCurrentUserId()
        val today = LocalDate.now()
        val startDate = today.minusDays((days - 1).toLong())
        val records = dailyRollupRepository.findByActivityDateBetween(currentUserId, startDate, today)
            .associateBy { it.activityDate }

        val filledDays = mutableListOf<DayRollupDto>()
        var curr = startDate
        while (!curr.isAfter(today)) {
            val record = records[curr]
            filledDays.add(
                DayRollupDto(
                    date = curr,
                    questionsAnswered = record?.questionsAnswered ?: 0,
                    correct = record?.correctAnswers ?: 0,
                    minutesPracticed = record?.minutesPracticed ?: 0,
                    minutesMocked = record?.minutesMocked ?: 0
                )
            )
            curr = curr.plusDays(1)
        }

        val last7Days = filledDays.takeLast(7)
        val rollingVolume = last7Days.sumOf { it.questionsAnswered }
        val rollingCorrect = last7Days.sumOf { it.correct }
        val rollingAccuracy = if (rollingVolume > 0) {
            BigDecimal.valueOf(100.0 * rollingCorrect / rollingVolume)
                .setScale(1, RoundingMode.HALF_UP)
                .toDouble()
        } else {
            0.0
        }

        return TrendsResponse(
            days = filledDays,
            rolling7 = Rolling7Stats(
                accuracy = rollingAccuracy,
                volume = rollingVolume
            )
        )
    }
}
