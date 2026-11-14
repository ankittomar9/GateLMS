package com.gatelms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.anatomy.repository.QuestionConceptTagRepository;
import com.gatelms.anatomy.service.CardSchedulingService;
import com.gatelms.dto.*;
import com.gatelms.dto.QuestionSummaryDto;
import com.gatelms.entity.*;
import com.gatelms.error.Exceptions.EntityNotFoundException;
import com.gatelms.error.Exceptions.ValidationException;
import com.gatelms.metrics.GateLmsMetrics;
import com.gatelms.repository.*;
import com.gatelms.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class PracticeService {
    private static final Logger logger = LoggerFactory.getLogger(PracticeService.class);

    private final QuestionRepository questionRepository;
    private final MockExamRepository mockExamRepository;
    private final MockAttemptRepository mockAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final TopicRepository topicRepository;
    private final QuestionProgressRepository questionProgressRepository;
    private final TopicMasteryRepository topicMasteryRepository;
    private final TopicMasteryService topicMasteryService;
    private final CurrentUserProvider currentUserProvider;
    private final GradingService gradingService;
    private final ObjectMapper objectMapper;
    private final GateLmsMetrics gateLmsMetrics;
    private final RollupService rollupService;
    private final PlanProgressService planProgressService;
    private final AchievementService achievementService;
    private final CardSchedulingService cardSchedulingService;
    private final QuestionConceptTagRepository questionConceptTagRepository;

    public PracticeService(
        QuestionRepository questionRepository,
        MockExamRepository mockExamRepository,
        MockAttemptRepository mockAttemptRepository,
        AttemptAnswerRepository attemptAnswerRepository,
        TopicRepository topicRepository,
        QuestionProgressRepository questionProgressRepository,
        TopicMasteryRepository topicMasteryRepository,
        TopicMasteryService topicMasteryService,
        CurrentUserProvider currentUserProvider,
        GradingService gradingService,
        ObjectMapper objectMapper,
        @Autowired(required = false) GateLmsMetrics gateLmsMetrics,
        @Autowired(required = false) RollupService rollupService,
        @Autowired(required = false) PlanProgressService planProgressService,
        @Autowired(required = false) @Lazy AchievementService achievementService,
        CardSchedulingService cardSchedulingService,
        @Autowired(required = false) QuestionConceptTagRepository questionConceptTagRepository
    ) {
        this.questionRepository = questionRepository;
        this.mockExamRepository = mockExamRepository;
        this.mockAttemptRepository = mockAttemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.topicRepository = topicRepository;
        this.questionProgressRepository = questionProgressRepository;
        this.topicMasteryRepository = topicMasteryRepository;
        this.topicMasteryService = topicMasteryService;
        this.currentUserProvider = currentUserProvider;
        this.gradingService = gradingService;
        this.objectMapper = objectMapper;
        this.gateLmsMetrics = gateLmsMetrics;
        this.rollupService = rollupService;
        this.planProgressService = planProgressService;
        this.achievementService = achievementService;
        this.cardSchedulingService = cardSchedulingService;
        this.questionConceptTagRepository = questionConceptTagRepository;
    }

    private List<String> getConceptTagsForQuestion(UUID questionId) {
        if (questionId == null || questionConceptTagRepository == null) return Collections.emptyList();
        return questionConceptTagRepository.findSlugsByQuestionId(questionId);
    }

    @Transactional
    public PracticeSessionResponse createSession(PracticeSessionCreateRequest request) {
        int count = Math.max(1, Math.min(50, request.count()));
        UUID userId = currentUserProvider.getCurrentUserId();

        Page<Question> questionsPage = questionRepository.findWithFilters(
            userId,
            (request.subjectId() != null && !request.subjectId().isBlank()) ? request.subjectId() : null,
            (request.topicId() != null && !request.topicId().isBlank()) ? request.topicId() : null,
            request.questionType() != null ? request.questionType().name() : null,
            request.year(),
            null, null, null,
            (request.curationStatus() != null && !request.curationStatus().isBlank()) ? request.curationStatus() : null,
            null, null, null,
            "created_at",
            PageRequest.of(0, count)
        );

        List<Question> initialList;
        if (request.marks() != null) {
            initialList = questionsPage.getContent().stream().filter(q -> Objects.equals(q.getMarks(), request.marks())).toList();
        } else {
            initialList = questionsPage.getContent();
        }

        List<Question> selectedQuestions;
        if (!initialList.isEmpty()) {
            selectedQuestions = initialList;
        } else {
            Page<Question> fallbackPage = questionRepository.findWithFilters(
                userId,
                (request.subjectId() != null && !request.subjectId().isBlank()) ? request.subjectId() : null,
                (request.topicId() != null && !request.topicId().isBlank()) ? request.topicId() : null,
                request.questionType() != null ? request.questionType().name() : null,
                request.year(),
                null, null, null, null, null, null, null,
                "created_at",
                PageRequest.of(0, count)
            );
            if (request.marks() != null) {
                List<Question> filtered = fallbackPage.getContent().stream()
                    .filter(q -> Objects.equals(q.getMarks(), request.marks())).toList();
                selectedQuestions = !filtered.isEmpty() ? filtered : fallbackPage.getContent();
            } else {
                selectedQuestions = fallbackPage.getContent();
            }
        }

        if (selectedQuestions.isEmpty()) {
            throw new ValidationException("No questions available for the specified practice criteria", Collections.emptyMap());
        }

        double totalMarks = selectedQuestions.stream().mapToInt(Question::getMarks).sum();
        String blueprintJson;
        try {
            blueprintJson = objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            blueprintJson = "{}";
        }

        String titleTopicOrSubject = request.topicId() != null ? request.topicId() : (request.subjectId() != null ? request.subjectId() : "General");
        MockExam mockExam = new MockExam();
        mockExam.setTitle("Practice Session - " + titleTopicOrSubject);
        mockExam.setDurationMinutes(0);
        mockExam.setTotalMarks(totalMarks);
        mockExam.setBlueprintJson(blueprintJson);
        mockExam.setMode(ExamMode.PRACTICE);

        MockExam savedExam = mockExamRepository.save(mockExam);

        MockAttempt attempt = new MockAttempt();
        attempt.setUserId(userId);
        attempt.setMockExam(savedExam);
        attempt.setStatus(MockAttemptStatus.IN_PROGRESS);

        MockAttempt savedAttempt = mockAttemptRepository.save(attempt);

        for (Question q : selectedQuestions) {
            AttemptAnswer ans = new AttemptAnswer();
            ans.setAttempt(savedAttempt);
            ans.setQuestion(q);
            ans.setState(AttemptAnswerState.NOT_VISITED);
            attemptAnswerRepository.save(ans);
        }

        List<UUID> qIds = selectedQuestions.stream().map(Question::getId).filter(Objects::nonNull).toList();
        Map<UUID, List<String>> tagMap = questionConceptTagRepository != null
            ? questionConceptTagRepository.findSlugsGroupedByQuestionIds(qIds)
            : Collections.emptyMap();

        List<PracticeQuestionDto> safeQuestionDtos = selectedQuestions.stream().map(q -> new PracticeQuestionDto(
            q.getId() != null ? q.getId() : UUID.randomUUID(),
            q.getExternalId(),
            q.getType(),
            q.getMarks(),
            q.getStatement(),
            q.getOptionsJson(),
            q.getTopic() != null ? q.getTopic().getId() : null,
            q.getSubject() != null ? q.getSubject().getId() : null,
            q.getYear(),
            tagMap.getOrDefault(q.getId(), Collections.emptyList())
        )).toList();

        return new PracticeSessionResponse(
            savedExam.getId(),
            savedAttempt.getId(),
            safeQuestionDtos,
            safeQuestionDtos.size()
        );
    }

    @Transactional
    public PracticeSessionResponse createDrillSession(
        List<Question> questions,
        String title,
        String source
    ) {
        double totalMarks = questions.stream().mapToInt(Question::getMarks).sum();
        MockExam mockExam = new MockExam();
        mockExam.setTitle(title);
        mockExam.setDurationMinutes(0);
        mockExam.setTotalMarks(totalMarks);
        mockExam.setBlueprintJson("{\"source\":\"" + source + "\",\"count\":" + questions.size() + "}");
        mockExam.setMode(ExamMode.WEAKNESS_DRILL);

        MockExam savedExam = mockExamRepository.save(mockExam);

        UUID userId = currentUserProvider.getCurrentUserId();
        MockAttempt attempt = new MockAttempt();
        attempt.setUserId(userId);
        attempt.setMockExam(savedExam);
        attempt.setStatus(MockAttemptStatus.IN_PROGRESS);

        MockAttempt savedAttempt = mockAttemptRepository.save(attempt);

        for (Question q : questions) {
            AttemptAnswer ans = new AttemptAnswer();
            ans.setAttempt(savedAttempt);
            ans.setQuestion(q);
            ans.setState(AttemptAnswerState.NOT_VISITED);
            attemptAnswerRepository.save(ans);
        }

        List<UUID> qIds = questions.stream().map(Question::getId).filter(Objects::nonNull).toList();
        Map<UUID, List<String>> tagMap = questionConceptTagRepository != null
            ? questionConceptTagRepository.findSlugsGroupedByQuestionIds(qIds)
            : Collections.emptyMap();

        List<PracticeQuestionDto> safeQuestionDtos = questions.stream().map(q -> new PracticeQuestionDto(
            q.getId() != null ? q.getId() : UUID.randomUUID(),
            q.getExternalId(),
            q.getType(),
            q.getMarks(),
            q.getStatement(),
            q.getOptionsJson(),
            q.getTopic() != null ? q.getTopic().getId() : null,
            q.getSubject() != null ? q.getSubject().getId() : null,
            q.getYear(),
            tagMap.getOrDefault(q.getId(), Collections.emptyList())
        )).toList();

        return new PracticeSessionResponse(
            savedExam.getId(),
            savedAttempt.getId(),
            safeQuestionDtos,
            safeQuestionDtos.size()
        );
    }

    @Transactional
    public PracticeVerdictResponse submitAnswer(UUID attemptId, PracticeAnswerSubmission submission) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        MockAttempt attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow(() ->
            new EntityNotFoundException("Practice session attempt not found with id: " + attemptId)
        );
        Question question = questionRepository.findById(submission.questionId()).orElseThrow(() ->
            new EntityNotFoundException("Question not found with id: " + submission.questionId())
        );

        UUID qId = question.getId() != null ? question.getId() : submission.questionId();
        long startNano = System.nanoTime();
        GradingService.GradingResult grading = gradingService.gradeQuestion(
            question,
            submission.selectedOptions(),
            submission.numericAnswer()
        );
        if (gateLmsMetrics != null) {
            gateLmsMetrics.recordGradingDuration("practice", Duration.ofNanos(System.nanoTime() - startNano));
        }

        AttemptAnswer ans = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, qId);
        if (ans == null) {
            ans = new AttemptAnswer();
            ans.setAttempt(attempt);
            ans.setQuestion(question);
        }

        ans.setSelectedOptions(submission.selectedOptions() != null ? submission.selectedOptions().trim() : null);
        ans.setNumericAnswer(submission.numericAnswer());
        ans.setState(AttemptAnswerState.ANSWERED);
        ans.setTimeSpentSeconds(submission.timeSpentSeconds());
        ans.setMarksAwarded(grading.marksAwarded());
        ans.setIsCorrect(grading.isCorrect());
        attemptAnswerRepository.save(ans);

        if (rollupService != null) {
            rollupService.recordAnswer(
                LocalDate.now(),
                grading.isCorrect(),
                submission.timeSpentSeconds(),
                "practice",
                currentUserId
            );
        }

        if (question.getTopic() != null && question.getTopic().getId() != null && planProgressService != null) {
            int secondsSpent = submission.timeSpentSeconds();
            int minutesSpent = Math.max(1, (secondsSpent + 59) / 60);
            try {
                planProgressService.onQuestionsAnswered(
                    LocalDate.now(),
                    question.getTopic().getId(),
                    1,
                    minutesSpent,
                    currentUserId
                );
            } catch (Exception e) {
                logger.warn("Plan progress hook failure ignored during practice grading: {}", e.getMessage(), e);
            }
        }

        QuestionProgress progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(currentUserId, qId).orElseGet(() ->
            new QuestionProgress(new QuestionProgressId(currentUserId, qId))
        );
        progress.setPass1Status(grading.isCorrect() ? Pass1Status.SOLVED : Pass1Status.FAILED);
        progress.setLastAttemptedAt(Instant.now());
        if (attempt.getMockExam() != null && attempt.getMockExam().getMode() == ExamMode.WEAKNESS_DRILL) {
            progress.setPass2Status(grading.isCorrect() ? Pass2Status.SOLVED : Pass2Status.FAILED);
        } else if (progress.getPass1Status() == Pass1Status.FAILED || progress.getPass1Status() == Pass1Status.ASSISTED) {
            progress.setPass2Status(Pass2Status.PENDING);
        }
        questionProgressRepository.save(progress);

        if (!grading.isCorrect()) {
            try {
                cardSchedulingService.enrollPrerequisiteCards(currentUserId, qId);
            } catch (Exception e) {
                logger.warn("Card auto-enrollment hook failure ignored for question {}: {}", qId, e.getMessage(), e);
            }
        }

        Topic topic = question.getTopic();
        int updatedMastery = 0;
        if (topic != null) {
            TopicMastery mastery = topicMasteryService.getMastery(topic.getId(), currentUserId);
            int newLevel = grading.isCorrect()
                ? Math.min(5, mastery.getMasteryLevel() + 1)
                : Math.max(0, mastery.getMasteryLevel() - 1);
            mastery.setMasteryLevel(newLevel);
            if (!grading.isCorrect()) {
                mastery.setNextRevisionDue(Instant.now().plus(Duration.ofDays(3)));
            }
            mastery.setLastRevisedAt(Instant.now());
            topicMasteryRepository.save(mastery);
            updatedMastery = newLevel;
        }

        if (achievementService != null) {
            try {
                achievementService.checkOnAnswerSubmitted(currentUserId);
                if (updatedMastery >= 5) {
                    achievementService.checkOnTopicMastery(updatedMastery, currentUserId);
                }
            } catch (Exception e) {
                logger.warn("Achievement check failed in practice grading: {}", e.getMessage(), e);
            }
        }

        List<String> qTags = getConceptTagsForQuestion(question.getId());
        return new PracticeVerdictResponse(
            grading.isCorrect(),
            grading.marksAwarded(),
            question.getCorrectOptions(),
            question.getNumericMin(),
            question.getNumericMax(),
            question.getExplanation(),
            updatedMastery,
            !grading.isCorrect(),
            qTags
        );
    }

    @Transactional
    public void skipQuestion(UUID attemptId, UUID questionId) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        MockAttempt attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow(() ->
            new EntityNotFoundException("Practice session attempt not found with id: " + attemptId)
        );
        Question question = questionRepository.findById(questionId).orElseThrow(() ->
            new EntityNotFoundException("Question not found with id: " + questionId)
        );

        UUID qId = question.getId() != null ? question.getId() : questionId;
        AttemptAnswer ans = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, qId);
        if (ans == null) {
            ans = new AttemptAnswer();
            ans.setAttempt(attempt);
            ans.setQuestion(question);
        }

        ans.setState(AttemptAnswerState.NOT_ANSWERED);
        attemptAnswerRepository.save(ans);
    }

    @Transactional
    public PracticeVerdictResponse revealQuestion(UUID attemptId, UUID questionId) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        MockAttempt attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow(() ->
            new EntityNotFoundException("Practice session attempt not found with id: " + attemptId)
        );
        Question question = questionRepository.findById(questionId).orElseThrow(() ->
            new EntityNotFoundException("Question not found with id: " + questionId)
        );

        UUID qId = question.getId() != null ? question.getId() : questionId;
        AttemptAnswer ans = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, qId);
        if (ans == null) {
            ans = new AttemptAnswer();
            ans.setAttempt(attempt);
            ans.setQuestion(question);
        }

        ans.setState(AttemptAnswerState.NOT_ANSWERED);
        ans.setIsCorrect(false);
        ans.setMarksAwarded(0.0);
        attemptAnswerRepository.save(ans);

        QuestionProgress progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(currentUserId, qId).orElseGet(() ->
            new QuestionProgress(new QuestionProgressId(currentUserId, qId))
        );
        progress.setPass1Status(Pass1Status.ASSISTED);
        progress.setPass2Status(Pass2Status.PENDING);
        progress.setLastAttemptedAt(Instant.now());
        questionProgressRepository.save(progress);

        int topicMastery = 0;
        if (question.getTopic() != null && question.getTopic().getId() != null) {
            topicMastery = topicMasteryRepository.findByIdUserIdAndIdTopicId(currentUserId, question.getTopic().getId())
                .map(TopicMastery::getMasteryLevel).orElse(0);
        }

        List<String> qTags = getConceptTagsForQuestion(question.getId());
        return new PracticeVerdictResponse(
            false,
            0.0,
            question.getCorrectOptions(),
            question.getNumericMin(),
            question.getNumericMax(),
            question.getExplanation(),
            topicMastery,
            false,
            qTags
        );
    }

    @Transactional
    public PracticeFinishResponse finishSession(UUID attemptId) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        MockAttempt attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow(() ->
            new EntityNotFoundException("Practice session attempt not found with id: " + attemptId)
        );

        attempt.setSubmittedAt(Instant.now());
        attempt.setStatus(MockAttemptStatus.SUBMITTED);
        mockAttemptRepository.save(attempt);

        if (gateLmsMetrics != null) {
            gateLmsMetrics.recordAttemptSubmitted("practice");
        }

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);

        int correct = (int) answers.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
        int wrong = (int) answers.stream().filter(a -> a.getState() == AttemptAnswerState.ANSWERED && Boolean.FALSE.equals(a.getIsCorrect())).count();
        int skipped = (int) answers.stream().filter(a -> a.getState() == AttemptAnswerState.NOT_ANSWERED || a.getState() == AttemptAnswerState.NOT_VISITED).count();
        int totalTime = answers.stream().mapToInt(AttemptAnswer::getTimeSpentSeconds).sum();

        Map<Topic, List<AttemptAnswer>> topicGroups = new HashMap<>();
        for (AttemptAnswer a : answers) {
            if (a.getQuestion() != null && a.getQuestion().getTopic() != null) {
                topicGroups.computeIfAbsent(a.getQuestion().getTopic(), k -> new ArrayList<>()).add(a);
            }
        }

        List<PracticeTopicSummary> topicSummaries = topicGroups.entrySet().stream().map(entry -> {
            Topic t = entry.getKey();
            List<AttemptAnswer> ansList = entry.getValue();
            int c = (int) ansList.stream().filter(a -> Boolean.TRUE.equals(a.getIsCorrect())).count();
            return new PracticeTopicSummary(t.getId(), t.getName(), c, ansList.size());
        }).toList();

        return new PracticeFinishResponse(
            correct,
            wrong,
            skipped,
            answers.size(),
            totalTime,
            topicSummaries
        );
    }
}
