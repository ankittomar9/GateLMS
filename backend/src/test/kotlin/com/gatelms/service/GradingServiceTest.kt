package com.gatelms.service

import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GradingServiceTest {

    private val gradingService = GradingService()

    @Test
    fun `gradeMcq returns true on exact letter match and false on mismatch or blank`() {
        assertTrue(gradingService.gradeMcq("B", "B"))
        assertTrue(gradingService.gradeMcq("b", "B"))
        assertTrue(gradingService.gradeMcq("  C  ", "c"))
        assertFalse(gradingService.gradeMcq("A", "B"))
        assertFalse(gradingService.gradeMcq(null, "B"))
        assertFalse(gradingService.gradeMcq("", "B"))
        assertFalse(gradingService.gradeMcq("B", null))
    }

    @Test
    fun `gradeMsq returns true on exact set equality with order-insensitivity and whitespace normalization`() {
        assertTrue(gradingService.gradeMsq("A,B", "B,A"))
        assertTrue(gradingService.gradeMsq("a, c, d", "D,A,C"))
        assertTrue(gradingService.gradeMsq("[\"A\",\"B\"]", "B,A"))
        assertTrue(gradingService.gradeMsq("A, B, C", "A,B,C"))
    }

    @Test
    fun `gradeMsq returns false on partial match or subset or superset`() {
        assertFalse(gradingService.gradeMsq("A", "A,B"))
        assertFalse(gradingService.gradeMsq("A,B,C", "A,B"))
        assertFalse(gradingService.gradeMsq("C", "A,B"))
        assertFalse(gradingService.gradeMsq("", "A,B"))
        assertFalse(gradingService.gradeMsq(null, "A,B"))
    }

    @Test
    fun `gradeNat returns true on inclusive bounds including exact min and exact max boundary values`() {
        assertTrue(gradingService.gradeNat(10.0, 5.0, 15.0))
        // Exact boundary values
        assertTrue(gradingService.gradeNat(5.0, 5.0, 15.0))
        assertTrue(gradingService.gradeNat(15.0, 5.0, 15.0))
        // Decimal precision
        assertTrue(gradingService.gradeNat(3.14, 3.10, 3.20))
    }

    @Test
    fun `gradeNat returns false when numericAnswer is outside bounds or missing`() {
        assertFalse(gradingService.gradeNat(4.99, 5.0, 15.0))
        assertFalse(gradingService.gradeNat(15.01, 5.0, 15.0))
        assertFalse(gradingService.gradeNat(null, 5.0, 15.0))
        assertFalse(gradingService.gradeNat(10.0, null, 15.0))
        assertFalse(gradingService.gradeNat(10.0, 5.0, null))
        assertFalse(gradingService.gradeNat(10.0, 15.0, 5.0)) // min > max
    }

    @Test
    fun `gradeQuestion awards positive marks on correct and exactly zero marks with no negative marking on wrong`() {
        val mcqQuestion = Question(
            type = QuestionType.MCQ,
            marks = 2,
            correctOptions = "B"
        )

        val correctResult = gradingService.gradeQuestion(mcqQuestion, "B", null)
        assertTrue(correctResult.isCorrect)
        assertEquals(2.0, correctResult.marksAwarded)

        val wrongResult = gradingService.gradeQuestion(mcqQuestion, "A", null)
        assertFalse(wrongResult.isCorrect)
        assertEquals(0.0, wrongResult.marksAwarded, "Practice mode must never penalize with negative marks")
    }

    @Test
    fun `gradeMcq handles json bracket formatted answers safely`() {
        assertTrue(gradingService.gradeMcq("[\"B\"]", "B"))
        assertTrue(gradingService.gradeMcq("B", "[\"B\"]"))
    }
}
