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
