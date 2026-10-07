package com.gatelms.entity

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Embeddable
data class DailyRollupId(
    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001"),

    @Column(name = "activity_date", nullable = false)
    var activityDate: LocalDate = LocalDate.now()
) : Serializable

@Entity
@Table(name = "daily_rollups")
class DailyRollup(
    @EmbeddedId
    var id: DailyRollupId = DailyRollupId(),

    @Column(name = "questions_answered", nullable = false)
    var questionsAnswered: Int = 0,

    @Column(name = "correct_answers", nullable = false)
    var correctAnswers: Int = 0,

    @Column(name = "minutes_practiced", nullable = false)
    var minutesPracticed: Int = 0,

    @Column(name = "minutes_mocked", nullable = false)
    var minutesMocked: Int = 0,

    @Column(name = "mock_attempts", nullable = false)
    var mockAttempts: Int = 0,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    val activityDate: LocalDate
        get() = id.activityDate

    val userId: UUID
        get() = id.userId

    constructor(
        userId: UUID,
        activityDate: LocalDate,
        questionsAnswered: Int = 0,
        correctAnswers: Int = 0,
        minutesPracticed: Int = 0,
        minutesMocked: Int = 0,
        mockAttempts: Int = 0,
        updatedAt: Instant = Instant.now()
    ) : this(
        id = DailyRollupId(userId = userId, activityDate = activityDate),
        questionsAnswered = questionsAnswered,
        correctAnswers = correctAnswers,
        minutesPracticed = minutesPracticed,
        minutesMocked = minutesMocked,
        mockAttempts = mockAttempts,
        updatedAt = updatedAt
    )
}
