package com.gatelms.service;

import com.gatelms.anatomy.repository.QuestionConceptTagRepository;
import com.gatelms.anatomy.repository.RetrievalCardStateRepository;
import com.gatelms.dto.PracticeDtos.PracticeSessionResponse;
import com.gatelms.dto.RevisionDtos.RevisionDrillRequest;
import com.gatelms.dto.RevisionDtos.RevisionDrillSource;
import com.gatelms.dto.RevisionDtos.RevisionOverviewDto;
import com.gatelms.entity.MistakeLog;
import com.gatelms.entity.Question;
import com.gatelms.repository.MistakeLogRepository;
import com.gatelms.repository.MockAttemptRepository;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.repository.TopicMasteryRepository;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class RevisionService {

    private final MistakeLogRepository mistakeLogRepository;
    private final TopicMasteryRepository topicMasteryRepository;
    private final QuestionRepository questionRepository;
    private final MockAttemptRepository mockAttemptRepository;
    private final RetrievalCardStateRepository retrievalCardStateRepository;
    private final PracticeService practiceService;
    private final CurrentUserProvider currentUserProvider;
    private final QuestionConceptTagRepository questionConceptTagRepository;

    public RevisionService(
        MistakeLogRepository mistakeLogRepository,
        TopicMasteryRepository topicMasteryRepository,
        QuestionRepository questionRepository,
        MockAttemptRepository mockAttemptRepository,
        RetrievalCardStateRepository retrievalCardStateRepository,
        PracticeService practiceService,
        CurrentUserProvider currentUserProvider,
        @Autowired(required = false) QuestionConceptTagRepository questionConceptTagRepository
    ) {
        this.mistakeLogRepository = mistakeLogRepository;
        this.topicMasteryRepository = topicMasteryRepository;
        this.questionRepository = questionRepository;
        this.mockAttemptRepository = mockAttemptRepository;
        this.retrievalCardStateRepository = retrievalCardStateRepository;
        this.practiceService = practiceService;
        this.currentUserProvider = currentUserProvider;
        this.questionConceptTagRepository = questionConceptTagRepository;
    }

    @Transactional(readOnly = true)
    public RevisionOverviewDto getOverview() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = Instant.now();
        long dueMistakes = mistakeLogRepository.countDueReviews(currentUserId, now);
        long dueTopics = topicMasteryRepository.countDueForRevision(currentUserId, now);
        long pass2Pending = questionRepository.countPass2Pending(currentUserId);
        long dueCards = retrievalCardStateRepository.countByIdUserIdAndDueAtLessThanEqual(currentUserId, now);
        long bookmarked = questionRepository.countBookmarked(currentUserId);
        long drillsCompleted = mockAttemptRepository.countCompletedDrills(currentUserId);

        return new RevisionOverviewDto(
            dueMistakes,
            dueTopics,
            pass2Pending,
            dueCards,
            bookmarked,
            drillsCompleted,
            now
        );
    }

    @Transactional
    public PracticeSessionResponse createDrill(RevisionDrillRequest request) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Instant now = Instant.now();
        int targetCount = Math.max(1, Math.min(50, request.count()));
        Pageable pageable = PageRequest.of(0, targetCount * 2);

        List<Question> questions;
        if (request.source() == RevisionDrillSource.DUE_MISTAKES) {
            List<MistakeLog> dueMistakes = mistakeLogRepository.findDueMistakesForDrill(
                currentUserId, now,
                (request.subjectId() != null && !request.subjectId().isBlank()) ? request.subjectId() : null,
                (request.topicId() != null && !request.topicId().isBlank()) ? request.topicId() : null,
                pageable
            );
            Set<UUID> seen = new HashSet<>();
            questions = new ArrayList<>();
            for (MistakeLog m : dueMistakes) {
                if (m.getQuestion() != null && seen.add(m.getQuestion().getId())) {
                    questions.add(m.getQuestion());
                    if (questions.size() >= targetCount) break;
                }
            }
        } else if (request.source() == RevisionDrillSource.PASS2) {
            questions = questionRepository.findPass2Candidates(
                currentUserId,
                (request.subjectId() != null && !request.subjectId().isBlank()) ? request.subjectId() : null,
                (request.topicId() != null && !request.topicId().isBlank()) ? request.topicId() : null,
                PageRequest.of(0, targetCount)
            );
        } else if (request.source() == RevisionDrillSource.BOOKMARKED) {
            questions = questionRepository.findBookmarkedCandidates(
                currentUserId,
                (request.subjectId() != null && !request.subjectId().isBlank()) ? request.subjectId() : null,
                (request.topicId() != null && !request.topicId().isBlank()) ? request.topicId() : null,
                PageRequest.of(0, targetCount)
            );
        } else {
            questions = questionRepository.findDueTopicCandidates(
                currentUserId, now,
                (request.subjectId() != null && !request.subjectId().isBlank()) ? request.subjectId() : null,
                (request.topicId() != null && !request.topicId().isBlank()) ? request.topicId() : null,
                PageRequest.of(0, targetCount)
            );
        }

        List<Question> effectiveQuestions;
        if (request.conceptSlug() != null && !request.conceptSlug().isBlank()) {
            List<Object[]> taggedInfos = questionConceptTagRepository != null
                ? questionConceptTagRepository.findTaggedQuestionInfos(List.of(request.conceptSlug()))
                : Collections.emptyList();
            Set<UUID> taggedIds = taggedInfos.stream()
                .map(row -> (UUID) row[1])
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            List<Question> matched = questions.stream().filter(q -> taggedIds.contains(q.getId())).toList();
            effectiveQuestions = !matched.isEmpty() ? matched : questions;
        } else {
            effectiveQuestions = questions;
        }

        if (effectiveQuestions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No candidates for this drill source");
        }

        String titleSuffix = (request.conceptSlug() != null && !request.conceptSlug().isBlank()) ? " · " + request.conceptSlug() : "";
        return practiceService.createDrillSession(
            effectiveQuestions,
            "Weakness Drill · " + request.source().name() + titleSuffix,
            request.source().name()
        );
    }

    @Transactional
    public PracticeSessionResponse redrillMistake(UUID mistakeId) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        MistakeLog mistake = mistakeLogRepository.findByIdAndUserId(mistakeId, currentUserId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Mistake log not found with id: " + mistakeId)
        );
        Question question = mistake.getQuestion();
        if (question == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Question associated with mistake not found");
        }

        String externalOrId = question.getExternalId() != null ? question.getExternalId() : question.getId().toString();
        return practiceService.createDrillSession(
            List.of(question),
            "Mistake Redrill - " + externalOrId,
            "MISTAKE_REDRILL"
        );
    }
}
