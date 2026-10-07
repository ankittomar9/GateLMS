package com.gatelms.service

import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import org.springframework.stereotype.Service

data class GradingResult(
    val isCorrect: Boolean,
    val marksAwarded: Double
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
