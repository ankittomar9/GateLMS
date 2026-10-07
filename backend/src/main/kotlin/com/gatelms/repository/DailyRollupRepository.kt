package com.gatelms.repository

import com.gatelms.entity.DailyRollup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DailyRollupRepository : JpaRepository<DailyRollup, LocalDate> {

    @Query("SELECT r FROM DailyRollup r WHERE r.activityDate >= :startDate AND r.activityDate <= :endDate ORDER BY r.activityDate ASC")
    fun findByActivityDateBetween(
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<DailyRollup>

    fun findAllByOrderByActivityDateDesc(): List<DailyRollup>

    @Modifying
    @Query(
        value = """
            INSERT INTO daily_rollups (activity_date, questions_answered, correct_answers, minutes_practiced, minutes_mocked, mock_attempts, updated_at)
            VALUES (:activityDate, :questionsAnswered, :correctAnswers, :minutesPracticed, :minutesMocked, :mockAttempts, CURRENT_TIMESTAMP)
            ON CONFLICT (activity_date) DO UPDATE SET
                questions_answered = daily_rollups.questions_answered + EXCLUDED.questions_answered,
                correct_answers = daily_rollups.correct_answers + EXCLUDED.correct_answers,
                minutes_practiced = daily_rollups.minutes_practiced + EXCLUDED.minutes_practiced,
                minutes_mocked = daily_rollups.minutes_mocked + EXCLUDED.minutes_mocked,
                mock_attempts = daily_rollups.mock_attempts + EXCLUDED.mock_attempts,
                updated_at = CURRENT_TIMESTAMP
        """,
        nativeQuery = true
    )
    fun upsertRollup(
        @Param("activityDate") activityDate: LocalDate,
        @Param("questionsAnswered") questionsAnswered: Int,
        @Param("correctAnswers") correctAnswers: Int,
        @Param("minutesPracticed") minutesPracticed: Int,
        @Param("minutesMocked") minutesMocked: Int,
        @Param("mockAttempts") mockAttempts: Int
    ): Int
}
