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
