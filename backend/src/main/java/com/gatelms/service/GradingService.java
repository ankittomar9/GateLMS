package com.gatelms.service;

import com.gatelms.entity.AttemptAnswerState;
import com.gatelms.entity.Question;
import com.gatelms.entity.QuestionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GradingService {

    public record GradingResult(
        boolean isCorrect,
        double marksAwarded
    ) {}

    public record MockGradingResult(
        boolean isAttempted,
        Boolean isCorrect,
        double marksAwarded,
        double penalty
    ) {}

    public boolean gradeMcq(String selectedOptions, String correctOptions) {
        if (selectedOptions == null || selectedOptions.isBlank() || correctOptions == null || correctOptions.isBlank()) {
            return false;
        }
        String selected = normalizeLetter(selectedOptions);
        String correct = normalizeLetter(correctOptions);
        return selected.equals(correct);
    }

    public boolean gradeMsq(String selectedOptions, String correctOptions) {
        if (selectedOptions == null || selectedOptions.isBlank() || correctOptions == null || correctOptions.isBlank()) {
            return false;
        }
        Set<String> selectedSet = parseOptionSet(selectedOptions);
        Set<String> correctSet = parseOptionSet(correctOptions);
        return !selectedSet.isEmpty() && selectedSet.equals(correctSet);
    }

    public boolean gradeNat(Double numericAnswer, Double numericMin, Double numericMax) {
        if (numericAnswer == null || numericMin == null || numericMax == null) return false;
        if (numericMin > numericMax) return false;
        return numericAnswer >= numericMin && numericAnswer <= numericMax;
    }

    public GradingResult gradeQuestion(Question question, String selectedOptions, Double numericAnswer) {
        boolean isCorrect;
        if (question.getType() == QuestionType.MCQ) {
            isCorrect = gradeMcq(selectedOptions, question.getCorrectOptions());
        } else if (question.getType() == QuestionType.MSQ) {
            isCorrect = gradeMsq(selectedOptions, question.getCorrectOptions());
        } else if (question.getType() == QuestionType.NAT) {
            isCorrect = gradeNat(numericAnswer, question.getNumericMin(), question.getNumericMax());
        } else {
            isCorrect = false;
        }

        double marksAwarded = isCorrect ? question.getMarks().doubleValue() : 0.0;
        return new GradingResult(isCorrect, marksAwarded);
    }

    public MockGradingResult gradeMockQuestion(
        Question question,
        String selectedOptions,
        Double numericAnswer,
        AttemptAnswerState state
    ) {
        boolean hasSelection = (selectedOptions != null && !selectedOptions.isBlank()) || numericAnswer != null;
        boolean isUnattempted = state == AttemptAnswerState.NOT_VISITED ||
            state == AttemptAnswerState.NOT_ANSWERED ||
            (state == AttemptAnswerState.MARKED_FOR_REVIEW && !hasSelection) ||
            !hasSelection;

        if (isUnattempted) {
            return new MockGradingResult(false, null, 0.0, 0.0);
        }

        boolean isCorrect;
        if (question.getType() == QuestionType.MCQ) {
            isCorrect = gradeMcq(selectedOptions, question.getCorrectOptions());
        } else if (question.getType() == QuestionType.MSQ) {
            isCorrect = gradeMsq(selectedOptions, question.getCorrectOptions());
        } else if (question.getType() == QuestionType.NAT) {
            isCorrect = gradeNat(numericAnswer, question.getNumericMin(), question.getNumericMax());
        } else {
            isCorrect = false;
        }

        if (isCorrect) {
            return new MockGradingResult(true, true, question.getMarks().doubleValue(), 0.0);
        }

        if (question.getType() == QuestionType.MCQ) {
            double penalty = BigDecimal.valueOf(question.getMarks().doubleValue() / 3.0)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
            double signedMarks = -penalty;
            return new MockGradingResult(true, false, signedMarks, penalty);
        } else {
            return new MockGradingResult(true, false, 0.0, 0.0);
        }
    }

    private String normalizeLetter(String raw) {
        return raw.replace("[", "").replace("]", "").replace("\"", "").trim().toUpperCase();
    }

    private Set<String> parseOptionSet(String raw) {
        String cleaned = raw.replace("[", "").replace("]", "").replace("\"", "");
        return Arrays.stream(cleaned.split(","))
            .map(s -> s.trim().toUpperCase())
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toSet());
    }
}
