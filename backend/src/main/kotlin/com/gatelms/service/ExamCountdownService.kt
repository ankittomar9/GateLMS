package com.gatelms.service

import com.gatelms.dto.ExamCountdownDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class ExamCountdownService(
    @Value("\${app.exam.date:2028-02-12}")
    private val examDateConfig: String
) {

    fun getCountdown(): ExamCountdownDto {
        val targetDate = try {
            LocalDate.parse(examDateConfig.trim())
        } catch (e: Exception) {
            LocalDate.of(2028, 2, 12)
        }

        val today = LocalDate.now()
        val daysRemaining = ChronoUnit.DAYS.between(today, targetDate)
        val clampedDays = maxOf(0L, daysRemaining)
        val weeksRemaining = clampedDays / 7L

        return ExamCountdownDto(
            examDate = targetDate.toString(),
            daysRemaining = clampedDays,
            weeksRemaining = weeksRemaining,
            source = "config"
        )
    }
}
