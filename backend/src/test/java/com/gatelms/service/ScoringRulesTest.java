package com.gatelms.service;

import com.gatelms.entity.AttemptAnswerState;
import com.gatelms.entity.Question;
import com.gatelms.entity.QuestionType;
import com.gatelms.service.GradingService.GradingResult;
import com.gatelms.service.GradingService.MockGradingResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ScoringRulesTest {

    private final GradingService gradingService = new GradingService();

    private Question createQuestion(QuestionType type, int marks, String correctOptions, Double min, Double max) {
        Question q = new Question();
        q.setType(type);
        q.setMarks(marks);
        q.setCorrectOptions(correctOptions);
        q.setNumericMin(min);
        q.setNumericMax(max);
        return q;
    }

    @Test
    public void mcq1MarkCorrectAwardsPlus1MarkWithZeroPenalty() {
        Question q = createQuestion(QuestionType.MCQ, 1, "A", null, null);
        MockGradingResult res = gradingService.gradeMockQuestion(q, "A", null, AttemptAnswerState.ANSWERED);
        assertTrue(res.isAttempted());
        assertEquals(Boolean.TRUE, res.isCorrect());
        assertEquals(1.0, res.marksAwarded());
        assertEquals(0.0, res.penalty());
    }

    @Test
    public void mcq1MarkWrongPenalizesMinus033With033RawPenalty() {
        Question q = createQuestion(QuestionType.MCQ, 1, "A", null, null);
        MockGradingResult res = gradingService.gradeMockQuestion(q, "B", null, AttemptAnswerState.ANSWERED);
        assertTrue(res.isAttempted());
        assertEquals(Boolean.FALSE, res.isCorrect());
        assertEquals(-0.33, res.marksAwarded());
        assertEquals(0.33, res.penalty());
    }

    @Test
    public void mcq2MarkCorrectAwardsPlus2MarksWithZeroPenalty() {
        Question q = createQuestion(QuestionType.MCQ, 2, "A", null, null);
        MockGradingResult res = gradingService.gradeMockQuestion(q, "A", null, AttemptAnswerState.ANSWERED);
        assertTrue(res.isAttempted());
        assertEquals(Boolean.TRUE, res.isCorrect());
        assertEquals(2.0, res.marksAwarded());
        assertEquals(0.0, res.penalty());
    }

    @Test
    public void mcq2MarkWrongPenalizesMinus067With067RawPenalty() {
        Question q = createQuestion(QuestionType.MCQ, 2, "A", null, null);
        MockGradingResult res = gradingService.gradeMockQuestion(q, "B", null, AttemptAnswerState.ANSWERED);
        assertTrue(res.isAttempted());
        assertEquals(Boolean.FALSE, res.isCorrect());
        assertEquals(-0.67, res.marksAwarded());
        assertEquals(0.67, res.penalty());
    }

    @Test
    public void msqCorrectAwardsFullMarksWithZeroPenalty() {
        Question q = createQuestion(QuestionType.MSQ, 2, "[\"A\",\"C\"]", null, null);
        MockGradingResult res = gradingService.gradeMockQuestion(q, "[\"C\",\"A\"]", null, AttemptAnswerState.ANSWERED);
        assertTrue(res.isAttempted());
        assertEquals(Boolean.TRUE, res.isCorrect());
        assertEquals(2.0, res.marksAwarded());
        assertEquals(0.0, res.penalty());
    }

    @Test
    public void msqPartialWrongAwardsZeroMarksWithZeroPenalty() {
        Question q = createQuestion(QuestionType.MSQ, 2, "[\"A\",\"C\"]", null, null);
        MockGradingResult res = gradingService.gradeMockQuestion(q, "[\"A\"]", null, AttemptAnswerState.ANSWERED);
        assertTrue(res.isAttempted());
        assertEquals(Boolean.FALSE, res.isCorrect());
        assertEquals(0.0, res.marksAwarded());
        assertEquals(0.0, res.penalty());
    }

    @Test
    public void natCorrectAwardsFullMarksWithZeroPenalty() {
        Question q = createQuestion(QuestionType.NAT, 2, null, 3.14, 3.15);
        MockGradingResult res = gradingService.gradeMockQuestion(q, null, 3.1415, AttemptAnswerState.ANSWERED);
        assertTrue(res.isAttempted());
        assertEquals(Boolean.TRUE, res.isCorrect());
        assertEquals(2.0, res.marksAwarded());
        assertEquals(0.0, res.penalty());
    }

    @Test
    public void natWrongAwardsZeroMarksWithZeroPenalty() {
        Question q = createQuestion(QuestionType.NAT, 2, null, 3.14, 3.15);
        MockGradingResult res = gradingService.gradeMockQuestion(q, null, 3.5, AttemptAnswerState.ANSWERED);
        assertTrue(res.isAttempted());
        assertEquals(Boolean.FALSE, res.isCorrect());
        assertEquals(0.0, res.marksAwarded());
        assertEquals(0.0, res.penalty());
    }

    @Test
    public void notVisitedIsUnattemptedWithZeroMarksAndZeroPenalty() {
        Question q = createQuestion(QuestionType.MCQ, 1, "A", null, null);
        MockGradingResult res = gradingService.gradeMockQuestion(q, null, null, AttemptAnswerState.NOT_VISITED);
        assertFalse(res.isAttempted());
        assertNull(res.isCorrect());
        assertEquals(0.0, res.marksAwarded());
        assertEquals(0.0, res.penalty());
    }

    @Test
    public void notAnsweredIsUnattemptedWithZeroMarksAndZeroPenalty() {
        Question q = createQuestion(QuestionType.MCQ, 1, "A", null, null);
        MockGradingResult res = gradingService.gradeMockQuestion(q, null, null, AttemptAnswerState.NOT_ANSWERED);
        assertFalse(res.isAttempted());
        assertNull(res.isCorrect());
        assertEquals(0.0, res.marksAwarded());
        assertEquals(0.0, res.penalty());
    }

    @Test
    public void markedForReviewWithoutAnswerIsUnattempted() {
        Question q = createQuestion(QuestionType.MCQ, 1, "A", null, null);
        MockGradingResult res = gradingService.gradeMockQuestion(q, null, null, AttemptAnswerState.MARKED_FOR_REVIEW);
        assertFalse(res.isAttempted());
        assertNull(res.isCorrect());
        assertEquals(0.0, res.marksAwarded());
        assertEquals(0.0, res.penalty());
    }

    @Test
    public void markedForReviewWithAnswerIsEvaluatedAsAttempted() {
        Question q = createQuestion(QuestionType.MCQ, 1, "A", null, null);
        MockGradingResult resCorrect = gradingService.gradeMockQuestion(q, "A", null, AttemptAnswerState.MARKED_FOR_REVIEW);
        assertTrue(resCorrect.isAttempted());
        assertEquals(Boolean.TRUE, resCorrect.isCorrect());
        assertEquals(1.0, resCorrect.marksAwarded());

        MockGradingResult resWrong = gradingService.gradeMockQuestion(q, "B", null, AttemptAnswerState.MARKED_FOR_REVIEW);
        assertTrue(resWrong.isAttempted());
        assertEquals(Boolean.FALSE, resWrong.isCorrect());
        assertEquals(-0.33, resWrong.marksAwarded());
        assertEquals(0.33, resWrong.penalty());
    }

    @Test
    public void negativeScoreAggregationMatchesFormula() {
        Question q1 = createQuestion(QuestionType.MCQ, 1, "A", null, null);
        Question q2 = createQuestion(QuestionType.MCQ, 2, "A", null, null);

        MockGradingResult r1 = gradingService.gradeMockQuestion(q1, "B", null, AttemptAnswerState.ANSWERED);
        MockGradingResult r2 = gradingService.gradeMockQuestion(q2, "B", null, AttemptAnswerState.ANSWERED);

        double totalMarksAwarded = r1.marksAwarded() + r2.marksAwarded();
        double totalPenalty = r1.penalty() + r2.penalty();

        assertEquals(-1.0, totalMarksAwarded, 0.001);
        assertEquals(1.0, totalPenalty, 0.001);
    }

    @Test
    public void practiceSessionGradingResultContainsIsCorrectAndMarks() {
        Question q = createQuestion(QuestionType.MCQ, 2, "A", null, null);
        GradingResult resCorrect = gradingService.gradeQuestion(q, "A", null);
        assertTrue(resCorrect.isCorrect());
        assertEquals(2.0, resCorrect.marksAwarded());

        GradingResult resWrong = gradingService.gradeQuestion(q, "B", null);
        assertFalse(resWrong.isCorrect());
        assertEquals(0.0, resWrong.marksAwarded());
    }

    @Test
    public void natEdgeConditionsAcceptBoundaryValues() {
        Question q = createQuestion(QuestionType.NAT, 1, null, 10.0, 20.0);
        GradingResult minBoundary = gradingService.gradeQuestion(q, null, 10.0);
        GradingResult maxBoundary = gradingService.gradeQuestion(q, null, 20.0);
        GradingResult belowMin = gradingService.gradeQuestion(q, null, 9.9999);
        GradingResult aboveMax = gradingService.gradeQuestion(q, null, 20.0001);

        assertTrue(minBoundary.isCorrect());
        assertTrue(maxBoundary.isCorrect());
        assertFalse(belowMin.isCorrect());
        assertFalse(aboveMax.isCorrect());
    }

    @Test
    public void msqOrderInsensitiveAndExactMatch() {
        Question q = createQuestion(QuestionType.MSQ, 2, "[\"A\",\"B\",\"D\"]", null, null);

        // Permutations that should succeed
        GradingResult p1 = gradingService.gradeQuestion(q, "[\"B\",\"A\",\"D\"]", null);
        GradingResult p2 = gradingService.gradeQuestion(q, "[\"D\",\"B\",\"A\"]", null);
        GradingResult p3 = gradingService.gradeQuestion(q, "A, B, D", null);
        GradingResult p4 = gradingService.gradeQuestion(q, "B, D, A", null);
        GradingResult p5 = gradingService.gradeQuestion(q, "[\"A\", \"B\", \"D\"]", null);

        assertTrue(p1.isCorrect());
        assertTrue(p2.isCorrect());
        assertTrue(p3.isCorrect());
        assertTrue(p4.isCorrect());
        assertTrue(p5.isCorrect());

        // Subset or superset that should fail
        GradingResult subset = gradingService.gradeQuestion(q, "[\"A\",\"B\"]", null);
        GradingResult superset = gradingService.gradeQuestion(q, "[\"A\",\"B\",\"C\",\"D\"]", null);

        assertFalse(subset.isCorrect());
        assertFalse(superset.isCorrect());
    }

    @Test
    public void gradeMcqNormalizesCaseAndWhitespace() {
        Question q = createQuestion(QuestionType.MCQ, 1, "a", null, null);
        GradingResult res1 = gradingService.gradeQuestion(q, " A ", null);
        GradingResult res2 = gradingService.gradeQuestion(q, "[\"A\"]", null);

        assertTrue(res1.isCorrect());
        assertTrue(res2.isCorrect());
    }
}
