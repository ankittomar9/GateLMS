package com.gatelms.service

import com.gatelms.entity.AttemptAnswerState
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

class ScoringRulesTest {

    private val gradingService = GradingService()

    @Test
    fun `mcq 1-mark correct awards plus 1 mark with zero penalty`() {
        val q = Question(type = QuestionType.MCQ, marks = 1, correctOptions = "A")
        val res = gradingService.gradeMockQuestion(q, "A", null, AttemptAnswerState.ANSWERED)
        assertTrue(res.isAttempted)
        assertEquals(true, res.isCorrect)
        assertEquals(1.0, res.marksAwarded)
        assertEquals(0.0, res.penalty)
    }

    @Test
    fun `mcq 1-mark wrong penalizes minus 0_33 with 0_33 raw penalty`() {
        val q = Question(type = QuestionType.MCQ, marks = 1, correctOptions = "A")
        val res = gradingService.gradeMockQuestion(q, "B", null, AttemptAnswerState.ANSWERED)
        assertTrue(res.isAttempted)
        assertEquals(false, res.isCorrect)
        assertEquals(-0.33, res.marksAwarded)
        assertEquals(0.33, res.penalty)
    }

    @Test
    fun `mcq 2-mark correct awards plus 2 marks with zero penalty`() {
        val q = Question(type = QuestionType.MCQ, marks = 2, correctOptions = "C")
        val res = gradingService.gradeMockQuestion(q, "C", null, AttemptAnswerState.ANSWERED)
        assertTrue(res.isAttempted)
        assertEquals(true, res.isCorrect)
        assertEquals(2.0, res.marksAwarded)
        assertEquals(0.0, res.penalty)
    }

    @Test
    fun `mcq 2-mark wrong penalizes minus 0_67 with 0_67 raw penalty`() {
        val q = Question(type = QuestionType.MCQ, marks = 2, correctOptions = "C")
        val res = gradingService.gradeMockQuestion(q, "D", null, AttemptAnswerState.ANSWERED)
        assertTrue(res.isAttempted)
        assertEquals(false, res.isCorrect)
        assertEquals(-0.67, res.marksAwarded)
        assertEquals(0.67, res.penalty)
    }

    @Test
    fun `msq exact set match awards full marks with zero penalty`() {
        val q = Question(type = QuestionType.MSQ, marks = 2, correctOptions = "A,B,C")
        val res = gradingService.gradeMockQuestion(q, "C,A,B", null, AttemptAnswerState.ANSWERED)
        assertTrue(res.isAttempted)
        assertEquals(true, res.isCorrect)
        assertEquals(2.0, res.marksAwarded)
        assertEquals(0.0, res.penalty)
    }

    @Test
    fun `msq partial selection yields zero marks and zero negative penalty`() {
        val q = Question(type = QuestionType.MSQ, marks = 2, correctOptions = "A,B")
        val res = gradingService.gradeMockQuestion(q, "A", null, AttemptAnswerState.ANSWERED)
        assertTrue(res.isAttempted)
        assertEquals(false, res.isCorrect)
        assertEquals(0.0, res.marksAwarded)
        assertEquals(0.0, res.penalty)
    }

    @Test
    fun `msq extra selection yields zero marks and zero negative penalty`() {
        val q = Question(type = QuestionType.MSQ, marks = 2, correctOptions = "A,B")
        val res = gradingService.gradeMockQuestion(q, "A,B,C", null, AttemptAnswerState.ANSWERED)
        assertTrue(res.isAttempted)
        assertEquals(false, res.isCorrect)
        assertEquals(0.0, res.marksAwarded)
        assertEquals(0.0, res.penalty)
    }

    @Test
    fun `nat within inclusive bounds awards full marks with zero penalty`() {
        val q = Question(type = QuestionType.NAT, marks = 2, numericMin = 10.0, numericMax = 20.0)
        val res = gradingService.gradeMockQuestion(q, null, 15.5, AttemptAnswerState.ANSWERED)
        assertTrue(res.isAttempted)
        assertEquals(true, res.isCorrect)
        assertEquals(2.0, res.marksAwarded)
        assertEquals(0.0, res.penalty)
    }

    @Test
    fun `nat at boundary values min and max awards full marks`() {
        val q = Question(type = QuestionType.NAT, marks = 1, numericMin = 5.0, numericMax = 10.0)
        val minRes = gradingService.gradeMockQuestion(q, null, 5.0, AttemptAnswerState.ANSWERED)
        val maxRes = gradingService.gradeMockQuestion(q, null, 10.0, AttemptAnswerState.ANSWERED)
        assertEquals(1.0, minRes.marksAwarded)
        assertEquals(1.0, maxRes.marksAwarded)
    }

    @Test
    fun `nat out of range or missing yields zero marks with zero penalty`() {
        val q = Question(type = QuestionType.NAT, marks = 2, numericMin = 10.0, numericMax = 20.0)
        val outRes = gradingService.gradeMockQuestion(q, null, 9.99, AttemptAnswerState.ANSWERED)
        assertTrue(outRes.isAttempted)
        assertEquals(false, outRes.isCorrect)
        assertEquals(0.0, outRes.marksAwarded)
        assertEquals(0.0, outRes.penalty)

        val missingRes = gradingService.gradeMockQuestion(q, null, null, AttemptAnswerState.NOT_ANSWERED)
        assertFalse(missingRes.isAttempted)
        assertNull(missingRes.isCorrect)
        assertEquals(0.0, missingRes.marksAwarded)
        assertEquals(0.0, missingRes.penalty)
    }

    @Test
    fun `unattempted questions across states evaluate to zero marks and zero penalty`() {
        val q = Question(type = QuestionType.MCQ, marks = 2, correctOptions = "B")

        val notVisited = gradingService.gradeMockQuestion(q, null, null, AttemptAnswerState.NOT_VISITED)
        assertFalse(notVisited.isAttempted)
        assertNull(notVisited.isCorrect)
        assertEquals(0.0, notVisited.marksAwarded)

        val notAnswered = gradingService.gradeMockQuestion(q, null, null, AttemptAnswerState.NOT_ANSWERED)
        assertFalse(notAnswered.isAttempted)
        assertNull(notAnswered.isCorrect)
        assertEquals(0.0, notAnswered.marksAwarded)

        val markedWithoutAnswer = gradingService.gradeMockQuestion(q, "", null, AttemptAnswerState.MARKED_FOR_REVIEW)
        assertFalse(markedWithoutAnswer.isAttempted)
        assertNull(markedWithoutAnswer.isCorrect)
        assertEquals(0.0, markedWithoutAnswer.marksAwarded)
    }

    @Test
    fun `aggregate total score applies floor at zero when negative penalty exceeds positive score`() {
        // 1 correct 1-mark MCQ, 4 wrong 1-mark MCQs
        val q1 = Question(type = QuestionType.MCQ, marks = 1, correctOptions = "A")
        val q2 = Question(type = QuestionType.MCQ, marks = 1, correctOptions = "A")
        val q3 = Question(type = QuestionType.MCQ, marks = 1, correctOptions = "A")
        val q4 = Question(type = QuestionType.MCQ, marks = 1, correctOptions = "A")
        val q5 = Question(type = QuestionType.MCQ, marks = 1, correctOptions = "A")

        val r1 = gradingService.gradeMockQuestion(q1, "A", null, AttemptAnswerState.ANSWERED) // +1.0
        val r2 = gradingService.gradeMockQuestion(q2, "B", null, AttemptAnswerState.ANSWERED) // -0.33
        val r3 = gradingService.gradeMockQuestion(q3, "B", null, AttemptAnswerState.ANSWERED) // -0.33
        val r4 = gradingService.gradeMockQuestion(q4, "B", null, AttemptAnswerState.ANSWERED) // -0.33
        val r5 = gradingService.gradeMockQuestion(q5, "B", null, AttemptAnswerState.ANSWERED) // -0.33

        val posScore = listOf(r1, r2, r3, r4, r5).filter { it.marksAwarded > 0 }.sumOf { it.marksAwarded }
        val negScore = listOf(r1, r2, r3, r4, r5).sumOf { it.penalty }

        assertEquals(1.0, posScore)
        assertEquals(1.32, BigDecimal.valueOf(negScore).setScale(2, RoundingMode.HALF_UP).toDouble())

        val totalScore = maxOf(0.0, BigDecimal.valueOf(posScore - negScore).setScale(2, RoundingMode.HALF_UP).toDouble())
        assertEquals(0.0, totalScore, "Total score must be clamped at floor 0.0")
    }

    @Test
    fun `per question marks awarded retains signed value`() {
        val q1 = Question(type = QuestionType.MCQ, marks = 1, correctOptions = "A")
        val q2 = Question(type = QuestionType.MCQ, marks = 2, correctOptions = "B")

        val res1 = gradingService.gradeMockQuestion(q1, "C", null, AttemptAnswerState.ANSWERED)
        val res2 = gradingService.gradeMockQuestion(q2, "C", null, AttemptAnswerState.ANSWERED)

        assertTrue(res1.marksAwarded < 0.0)
        assertEquals(-0.33, res1.marksAwarded)
        assertTrue(res2.marksAwarded < 0.0)
        assertEquals(-0.67, res2.marksAwarded)
    }
}
