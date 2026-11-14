package com.gatelms.service;

import com.gatelms.anatomy.repository.QuestionConceptTagRepository;
import com.gatelms.dto.PracticeDtos.SimilarQuestionsResponseDto;
import com.gatelms.dto.QuestionDtos.QuestionSummaryDto;
import com.gatelms.entity.Question;
import com.gatelms.entity.QuestionProgress;
import com.gatelms.repository.QuestionProgressRepository;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SimilarQuestionsService {

    private final QuestionRepository questionRepository;
    private final QuestionProgressRepository questionProgressRepository;
    private final QuestionConceptTagRepository questionConceptTagRepository;
    private final QuestionService questionService;
    private final CurrentUserProvider currentUserProvider;

    public SimilarQuestionsService(
        QuestionRepository questionRepository,
        QuestionProgressRepository questionProgressRepository,
        QuestionConceptTagRepository questionConceptTagRepository,
        QuestionService questionService,
        CurrentUserProvider currentUserProvider
    ) {
        this.questionRepository = questionRepository;
        this.questionProgressRepository = questionProgressRepository;
        this.questionConceptTagRepository = questionConceptTagRepository;
        this.questionService = questionService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public SimilarQuestionsResponseDto getSimilarQuestions(UUID questionId, int limit) {
        Question sourceQuestion = questionRepository.findById(questionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found: " + questionId));

        UUID userId = currentUserProvider.getCurrentUserId();
        Set<UUID> solvedQuestionIds = questionProgressRepository.findSolvedQuestionIdsForUser(userId);
        String sourceNormalizedStatement = normalizeStatement(sourceQuestion.getStatement());

        // 1. Fetch concept candidates sharing slugs
        List<String> sourceSlugs = questionConceptTagRepository.findSlugsByQuestionId(questionId);
        List<SimilarQuestionRanker.CandidateInfo> conceptCandidates;
        if (!sourceSlugs.isEmpty()) {
            List<Object[]> rows = questionConceptTagRepository.findConceptCandidatesWithSharedSlugCount(sourceSlugs, questionId);
            conceptCandidates = rows.stream().map(row -> {
                UUID qId = (UUID) row[0];
                int sharedCount = ((Number) row[1]).intValue();
                Integer year = row[2] != null ? ((Number) row[2]).intValue() : null;
                long createdAt = row[3] != null ? ((Instant) row[3]).toEpochMilli() : 0L;
                String stmt = row.length > 4 ? (String) row[4] : null;
                String normStmt = stmt != null ? normalizeStatement(stmt) : null;
                return new SimilarQuestionRanker.CandidateInfo(qId, sharedCount, year, createdAt, normStmt);
            }).toList();
        } else {
            conceptCandidates = Collections.emptyList();
        }

        // 2. Fetch topic candidates
        String topicId = sourceQuestion.getTopic() != null ? sourceQuestion.getTopic().getId() : null;
        List<SimilarQuestionRanker.CandidateInfo> topicCandidates;
        if (topicId != null && !topicId.isBlank()) {
            List<Object[]> rows = questionRepository.findTopicCandidates(topicId, questionId);
            topicCandidates = rows.stream().map(row -> {
                UUID qId = (UUID) row[0];
                Integer year = row[1] != null ? ((Number) row[1]).intValue() : null;
                long createdAt = row[2] != null ? ((Instant) row[2]).toEpochMilli() : 0L;
                String stmt = row.length > 3 ? (String) row[3] : null;
                String normStmt = stmt != null ? normalizeStatement(stmt) : null;
                return new SimilarQuestionRanker.CandidateInfo(qId, 0, year, createdAt, normStmt);
            }).toList();
        } else {
            topicCandidates = Collections.emptyList();
        }

        // 3. Execute pure ranker
        SimilarQuestionRanker.RankedResult result = SimilarQuestionRanker.rankCandidates(
            questionId,
            conceptCandidates,
            topicCandidates,
            solvedQuestionIds,
            limit,
            sourceNormalizedStatement
        );

        if (result.questionIds().isEmpty()) {
            return new SimilarQuestionsResponseDto(Collections.emptyList(), result.similarityBasis());
        }

        Map<UUID, Question> questionsMap = questionRepository.findAllById(result.questionIds()).stream()
            .collect(Collectors.toMap(Question::getId, q -> q));
        Map<UUID, QuestionProgress> progressMap = questionProgressRepository.findAllByUserIdAndQuestionIdIn(userId, result.questionIds()).stream()
            .collect(Collectors.toMap(p -> p.getId().getQuestionId(), p -> p));

        List<QuestionSummaryDto> questionDtos = result.questionIds().stream().map(id -> {
            Question q = questionsMap.get(id);
            if (q == null) return null;
            return questionService.toSummaryDto(q, progressMap.get(id));
        }).filter(Objects::nonNull).toList();

        return new SimilarQuestionsResponseDto(questionDtos, result.similarityBasis());
    }

    public static String normalizeStatement(String statement) {
        if (statement == null) return "";
        return statement.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
