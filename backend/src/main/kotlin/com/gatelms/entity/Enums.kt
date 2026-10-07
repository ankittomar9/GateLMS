package com.gatelms.entity

enum class QuestionType {
    MCQ,
    MSQ,
    NAT
}

enum class Pass1Status {
    PENDING,
    SOLVED,
    ASSISTED,
    FAILED
}

enum class Pass2Status {
    PENDING,
    SOLVED,
    FAILED
}

enum class QuestionSource {
    SAMPLE,
    SO_DERIVED,
    AUTHORED,
    GATE_PYP,
    APT
}

enum class CurationStatus {
    UNREVIEWED,
    ACCEPTED,
    REJECTED
}

enum class QuestionDifficulty {
    EASY,
    MEDIUM,
    HARD
}

enum class ExamMode {
    MOCK,
    PRACTICE,
    WEAKNESS_DRILL
}

enum class MockAttemptStatus {
    IN_PROGRESS,
    SUBMITTED,
    ABANDONED
}

enum class AttemptAnswerState {
    NOT_VISITED,
    NOT_ANSWERED,
    ANSWERED,
    MARKED_FOR_REVIEW,
    ANSWERED_AND_MARKED_FOR_REVIEW,
    ANSWERED_AND_MARKED
}

enum class FinishedReason {
    SUBMITTED,
    AUTO_SUBMITTED,
    ABANDONED
}

enum class MistakeErrorCategory {
    CONCEPTUAL,
    CALCULATION,
    MSQ_TRAP,
    MISREAD
}

enum class PlanStrategy {
    WEIGHTED,
    WEAKNESS_FIRST,
    LINEAR
}

enum class PlanItemStatus {
    OPEN,
    DONE,
    SKIPPED
}

enum class NotificationType {
    REVISION_DUE,
    STREAK_AT_RISK,
    PLAN_DUE,
    ACHIEVEMENT_EARNED
}

enum class AchievementCode(val title: String, val description: String) {
    FIRST_QUESTION("First Step", "Answered your first practice or test question"),
    FIRST_MOCK("Trial by Fire", "Completed your first mock test"),
    QUESTIONS_100("Centurion", "Answered 100 distinct questions"),
    QUESTIONS_500("Half Grand", "Answered 500 distinct questions"),
    QUESTIONS_1000("Grand Master", "Answered 1,000 distinct questions"),
    STREAK_3("Three-Day Ignition", "Maintained a 3-day study streak"),
    STREAK_7("Weekly Discipline", "Maintained a 7-day study streak"),
    STREAK_30("Monthly Momentum", "Maintained a 30-day study streak"),
    MOCK_90PCT("Elite Scorer", "Scored 90% or higher on a mock examination"),
    TOPIC_MASTERY_5("Topic Grandmaster", "Achieved mastery level 5 on any topic"),
    CURATOR_500("Master Curator", "Reached 500 question curation decisions"),
    REVISIONER_50("Spaced Repetition Pro", "Reviewed 50 mistake entries in notebook")
}
