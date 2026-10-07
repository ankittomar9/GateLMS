package com.gatelms.repository

import com.gatelms.entity.DailyRollup
import com.gatelms.entity.DailyRollupId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface DailyRollupRepository : JpaRepository<DailyRollup, DailyRollupId> {

    @Query("SELECT r FROM DailyRollup r WHERE r.id.userId = :userId AND r.id.activityDate >= :startDate AND r.id.activityDate <= :endDate ORDER BY r.id.activityDate ASC")
    fun findByActivityDateBetween(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate
    ): List<DailyRollup>

    @Query("SELECT r FROM DailyRollup r WHERE r.id.userId = :userId ORDER BY r.id.activityDate DESC")
    fun findAllByUserIdOrderByActivityDateDesc(@Param("userId") userId: UUID): List<DailyRollup>

    @Modifying
    @Query(
        value = """
            INSERT INTO daily_rollups (user_id, activity_date, questions_answered, correct_answers, minutes_practiced, minutes_mocked, mock_attempts, updated_at)
            VALUES (:userId, :activityDate, :questionsAnswered, :correctAnswers, :minutesPracticed, :minutesMocked, :mockAttempts, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, activity_date) DO UPDATE SET
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
        @Param("userId") userId: UUID,
        @Param("activityDate") activityDate: LocalDate,
        @Param("questionsAnswered") questionsAnswered: Int,
        @Param("correctAnswers") correctAnswers: Int,
        @Param("minutesPracticed") minutesPracticed: Int,
        @Param("minutesMocked") minutesMocked: Int,
        @Param("mockAttempts") mockAttempts: Int
    ): Int
}
