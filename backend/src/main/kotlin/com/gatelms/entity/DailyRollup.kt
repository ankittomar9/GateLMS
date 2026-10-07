package com.gatelms.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "daily_rollups")
class DailyRollup(
    @Id
    @Column(name = "activity_date", nullable = false)
    val activityDate: LocalDate,

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
)
