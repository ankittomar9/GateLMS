package com.gatelms.service

import com.gatelms.entity.AttemptAnswerState
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

data class GradingResult(
    val isCorrect: Boolean,
    val marksAwarded: Double
)

data class MockGradingResult(
    val isAttempted: Boolean,
    val isCorrect: Boolean?,
    val marksAwarded: Double,
    val penalty: Double
)

@Service
class GradingService {

    fun gradeMcq(selectedOptions: String?, correctOptions: String?): Boolean {
        if (selectedOptions.isNullOrBlank() || correctOptions.isNullOrBlank()) return false
        val selected = normalizeLetter(selectedOptions)
        val correct = normalizeLetter(correctOptions)
        return selected == correct
    }

    fun gradeMsq(selectedOptions: String?, correctOptions: String?): Boolean {
        if (selectedOptions.isNullOrBlank() || correctOptions.isNullOrBlank()) return false
        val selectedSet = parseOptionSet(selectedOptions)
        val correctSet = parseOptionSet(correctOptions)
        return selectedSet.isNotEmpty() && selectedSet == correctSet
    }

    fun gradeNat(numericAnswer: Double?, numericMin: Double?, numericMax: Double?): Boolean {
        if (numericAnswer == null || numericMin == null || numericMax == null) return false
        if (numericMin > numericMax) return false
        return numericAnswer in numericMin..numericMax
    }

    fun gradeQuestion(question: Question, selectedOptions: String?, numericAnswer: Double?): GradingResult {
        val isCorrect = when (question.type) {
            QuestionType.MCQ -> gradeMcq(selectedOptions, question.correctOptions)
            QuestionType.MSQ -> gradeMsq(selectedOptions, question.correctOptions)
            QuestionType.NAT -> gradeNat(numericAnswer, question.numericMin, question.numericMax)
        }
        val marksAwarded = if (isCorrect) question.marks.toDouble() else 0.0
        return GradingResult(isCorrect = isCorrect, marksAwarded = marksAwarded)
    }

    fun gradeMockQuestion(
        question: Question,
        selectedOptions: String?,
        numericAnswer: Double?,
        state: AttemptAnswerState?
    ): MockGradingResult {
        val hasSelection = !selectedOptions.isNullOrBlank() || numericAnswer != null
        val isUnattempted = state == AttemptAnswerState.NOT_VISITED ||
                state == AttemptAnswerState.NOT_ANSWERED ||
                (state == AttemptAnswerState.MARKED_FOR_REVIEW && !hasSelection) ||
                !hasSelection

        if (isUnattempted) {
            return MockGradingResult(
                isAttempted = false,
                isCorrect = null,
                marksAwarded = 0.0,
                penalty = 0.0
            )
        }

        val isCorrect = when (question.type) {
            QuestionType.MCQ -> gradeMcq(selectedOptions, question.correctOptions)
            QuestionType.MSQ -> gradeMsq(selectedOptions, question.correctOptions)
            QuestionType.NAT -> gradeNat(numericAnswer, question.numericMin, question.numericMax)
        }

        if (isCorrect) {
            return MockGradingResult(
                isAttempted = true,
                isCorrect = true,
                marksAwarded = question.marks.toDouble(),
                penalty = 0.0
            )
        }

        // Incorrect answer
        if (question.type == QuestionType.MCQ) {
            val penalty = BigDecimal.valueOf(question.marks.toDouble() / 3.0)
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
            val signedMarks = -penalty
            return MockGradingResult(
                isAttempted = true,
                isCorrect = false,
                marksAwarded = signedMarks,
                penalty = penalty
            )
        } else {
            // MSQ or NAT: zero negative marking
            return MockGradingResult(
                isAttempted = true,
                isCorrect = false,
                marksAwarded = 0.0,
                penalty = 0.0
            )
        }
    }

    private fun normalizeLetter(raw: String): String {
        return raw.replace("[", "").replace("]", "").replace("\"", "").trim().uppercase()
    }

    private fun parseOptionSet(raw: String): Set<String> {
        val cleaned = raw.replace("[", "").replace("]", "").replace("\"", "")
        return cleaned.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
