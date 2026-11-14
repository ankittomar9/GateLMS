package com.gatelms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.anatomy.repository.QuestionConceptTagRepository;
import com.gatelms.anatomy.service.CardSchedulingService;
import com.gatelms.dto.MockDtos.*;
import com.gatelms.dto.PracticeDtos.PracticeQuestionDto;
import com.gatelms.entity.*;
import com.gatelms.metrics.GateLmsMetrics;
import com.gatelms.repository.*;
import com.gatelms.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MockTestService {
    private static final Logger logger = LoggerFactory.getLogger(MockTestService.class);

    private final MockExamRepository mockExamRepository;
    private final MockAttemptRepository mockAttemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final MockAssemblyService mockAssemblyService;
    private final GradingService gradingService;
    private final ObjectMapper objectMapper;
    private final CurrentUserProvider currentUserProvider;
    private final GateLmsMetrics gateLmsMetrics;
    private final RollupService rollupService;
    private final PlanProgressService planProgressService;
    private final AchievementService achievementService;
    private final CardSchedulingService cardSchedulingService;
    private final QuestionConceptTagRepository questionConceptTagRepository;

    public MockTestService(
        MockExamRepository mockExamRepository,
        MockAttemptRepository mockAttemptRepository,
        AttemptAnswerRepository attemptAnswerRepository,
        QuestionRepository questionRepository,
        SubjectRepository subjectRepository,
        MockAssemblyService mockAssemblyService,
        GradingService gradingService,
        ObjectMapper objectMapper,
        CurrentUserProvider currentUserProvider,
        @Autowired(required = false) GateLmsMetrics gateLmsMetrics,
        @Autowired(required = false) RollupService rollupService,
        @Autowired(required = false) PlanProgressService planProgressService,
        @Autowired(required = false) AchievementService achievementService,
        CardSchedulingService cardSchedulingService,
        @Autowired(required = false) QuestionConceptTagRepository questionConceptTagRepository
    ) {
        this.mockExamRepository = mockExamRepository;
        this.mockAttemptRepository = mockAttemptRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.mockAssemblyService = mockAssemblyService;
        this.gradingService = gradingService;
        this.objectMapper = objectMapper;
        this.currentUserProvider = currentUserProvider;
        this.gateLmsMetrics = gateLmsMetrics;
        this.rollupService = rollupService;
        this.planProgressService = planProgressService;
        this.achievementService = achievementService;
        this.cardSchedulingService = cardSchedulingService;
        this.questionConceptTagRepository = questionConceptTagRepository;
    }

    @Transactional
    public MockTemplateSummaryDto createTemplate(CreateMockTemplateRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }

        List<BlueprintSection> sections = req.sections() != null ? req.sections() : Collections.emptyList();
        for (BlueprintSection section : sections) {
            if (!subjectRepository.existsById(section.subjectId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject not found: " + section.subjectId());
            }
        }

        double totalMarksCalculated = req.totalMarks() != null ? req.totalMarks() : sections.stream().mapToDouble(s -> {
            int qCount = s.mcq() + s.msq() + s.nat();
            int marksPerQ = s.marks() != null ? s.marks() : 1;
            return (double) qCount * marksPerQ;
        }).sum();

        MockBlueprint blueprint = new MockBlueprint(
            req.title(),
            null, null,
            sections,
            req.questionPool(),
            req.shuffle()
        );

        String blueprintJson;
        try {
            blueprintJson = objectMapper.writeValueAsString(blueprint);
        } catch (Exception e) {
            blueprintJson = "{}";
        }

        MockExam exam = new MockExam();
        exam.setTitle(req.title());
        exam.setDurationMinutes(req.durationMinutes());
        exam.setTotalMarks(totalMarksCalculated);
        exam.setBlueprintJson(blueprintJson);
        exam.setMode(ExamMode.MOCK);

        MockExam saved = mockExamRepository.save(exam);
        int totalQuestions = sections.stream().mapToInt(s -> s.mcq() + s.msq() + s.nat()).sum();

        return new MockTemplateSummaryDto(
            saved.getId(),
            saved.getTitle(),
            saved.getDurationMinutes() != null ? saved.getDurationMinutes() : 180,
            saved.getTotalMarks() != null ? saved.getTotalMarks() : 100.0,
            totalQuestions,
            saved.getMode() != null ? saved.getMode().name() : "MOCK",
            saved.getCreatedAt(),
            sections,
            null,
            0,
            Collections.emptyList()
        );
    }

    @Transactional
    public MockTemplateSummaryDto generatePypMock(int year) {
        List<Question> pypQuestions = questionRepository.findPypByYear(year);
        if (pypQuestions.isEmpty()) {
            pypQuestions = questionRepository.findByYear(year);
        }
        if (pypQuestions.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No questions found for GATE year " + year);
        }

        List<String> shortfallWarnings = new ArrayList<>();
        Subject gaSubject = subjectRepository.findBySlug("general-aptitude");
        String gaSubjectId = gaSubject != null ? gaSubject.getId() : "subj_aptitude";

        List<Question> gaQuestions = pypQuestions.stream()
            .filter(q -> (q.getSubject() != null && gaSubjectId.equals(q.getSubject().getId())) ||
                (q.getSubject() != null && "general-aptitude".equals(q.getSubject().getSlug())))
            .toList();

        List<Question> coreQuestions = pypQuestions.stream()
            .filter(q -> !((q.getSubject() != null && gaSubjectId.equals(q.getSubject().getId())) ||
                (q.getSubject() != null && "general-aptitude".equals(q.getSubject().getSlug()))))
            .toList();

        if (gaQuestions.size() < 10) {
            shortfallWarnings.add("General Aptitude: requested 10 questions but only " + gaQuestions.size() + " available in pool");
        }
        if (coreQuestions.size() < 55) {
            shortfallWarnings.add("CSE Core: requested 55 questions but only " + coreQuestions.size() + " available in pool");
        }

        Map<Subject, List<Question>> bySubject = new HashMap<>();
        for (Question q : pypQuestions) {
            Subject subj = q.getSubject() != null ? q.getSubject() : subjectRepository.findById(gaSubjectId).orElse(null);
            if (subj != null) {
                bySubject.computeIfAbsent(subj, k -> new ArrayList<>()).add(q);
            }
        }

        List<BlueprintSection> sections = new ArrayList<>();
        for (Map.Entry<Subject, List<Question>> entry : bySubject.entrySet()) {
            Subject subj = entry.getKey();
            List<Question> qList = entry.getValue();
            int mcqCount = (int) qList.stream().filter(q -> q.getType() == QuestionType.MCQ).count();
            int msqCount = (int) qList.stream().filter(q -> q.getType() == QuestionType.MSQ).count();
            int natCount = (int) qList.stream().filter(q -> q.getType() == QuestionType.NAT).count();

            sections.add(new BlueprintSection(
                subj.getId(),
                mcqCount,
                msqCount,
                natCount,
                null
            ));
        }

        int totalQuestions = pypQuestions.size();
        double totalMarks = pypQuestions.stream().mapToInt(Question::getMarks).sum();

        MockBlueprint blueprint = new MockBlueprint(
            "GATE " + year + " Official Paper",
            year,
            "GATE_PYP",
            sections,
            null,
            false
        );

        String blueprintJson;
        try {
            blueprintJson = objectMapper.writeValueAsString(blueprint);
        } catch (Exception e) {
            blueprintJson = "{}";
        }

        String title = "GATE " + year + " Official Paper";
        MockExam existing = mockExamRepository.findFirstByTitle(title);
        MockExam saved;
        if (existing != null) {
            existing.setBlueprintJson(blueprintJson);
            existing.setTotalMarks(totalMarks);
            existing.setDurationMinutes(180);
            saved = mockExamRepository.save(existing);
        } else {
            MockExam exam = new MockExam();
            exam.setTitle(title);
            exam.setDurationMinutes(180);
            exam.setTotalMarks(totalMarks);
            exam.setBlueprintJson(blueprintJson);
            exam.setMode(ExamMode.MOCK);
            saved = mockExamRepository.save(exam);
        }

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        Double bestScore = mockAttemptRepository.findMaxScoreByMockExamId(saved.getId(), currentUserId);
        int attemptCount = (int) mockAttemptRepository.countByMockExamIdAndUserIdAndStatus(saved.getId(), currentUserId, MockAttemptStatus.SUBMITTED);

        return new MockTemplateSummaryDto(
            saved.getId(),
            saved.getTitle(),
            saved.getDurationMinutes() != null ? saved.getDurationMinutes() : 180,
            saved.getTotalMarks() != null ? saved.getTotalMarks() : 100.0,
            totalQuestions,
            saved.getMode() != null ? saved.getMode().name() : "MOCK",
            saved.getCreatedAt(),
            sections,
            bestScore,
            attemptCount,
            shortfallWarnings
        );
    }

    @Transactional(readOnly = true)
    public List<MockTemplateSummaryDto> listTemplates() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        List<MockExam> exams = mockExamRepository.findByModeOrderByCreatedAtDesc(ExamMode.MOCK);
        return exams.stream().map(exam -> {
            MockBlueprint blueprint = mockAssemblyService.parseBlueprint(exam.getBlueprintJson());
            int totalQuestions = blueprint.sections() != null
                ? blueprint.sections().stream().mapToInt(s -> s.mcq() + s.msq() + s.nat()).sum()
                : 0;
            Double bestScore = mockAttemptRepository.findMaxScoreByMockExamId(exam.getId(), currentUserId);
            int attemptCount = (int) mockAttemptRepository.countByMockExamIdAndUserIdAndStatus(exam.getId(), currentUserId, MockAttemptStatus.SUBMITTED);

            return new MockTemplateSummaryDto(
                exam.getId(),
                exam.getTitle(),
                exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 180,
                exam.getTotalMarks() != null ? exam.getTotalMarks() : 100.0,
                totalQuestions,
                exam.getMode() != null ? exam.getMode().name() : "MOCK",
                exam.getCreatedAt(),
                blueprint.sections() != null ? blueprint.sections() : Collections.emptyList(),
                bestScore,
                attemptCount,
                Collections.emptyList()
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public MockTemplateSummaryDto getTemplate(UUID id) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        MockExam exam = mockExamRepository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Mock template not found")
        );

        MockBlueprint blueprint = mockAssemblyService.parseBlueprint(exam.getBlueprintJson());
        int totalQuestions = blueprint.sections() != null
            ? blueprint.sections().stream().mapToInt(s -> s.mcq() + s.msq() + s.nat()).sum()
            : 0;
        Double bestScore = mockAttemptRepository.findMaxScoreByMockExamId(exam.getId(), currentUserId);
        int attemptCount = (int) mockAttemptRepository.countByMockExamIdAndUserIdAndStatus(exam.getId(), currentUserId, MockAttemptStatus.SUBMITTED);

        return new MockTemplateSummaryDto(
            exam.getId(),
            exam.getTitle(),
            exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 180,
            exam.getTotalMarks() != null ? exam.getTotalMarks() : 100.0,
            totalQuestions,
            exam.getMode() != null ? exam.getMode().name() : "MOCK",
            exam.getCreatedAt(),
            blueprint.sections() != null ? blueprint.sections() : Collections.emptyList(),
            bestScore,
            attemptCount,
            Collections.emptyList()
        );
    }

    @Transactional
    public StartMockAttemptResponse startAttempt(UUID mockId) {
        MockExam exam = mockExamRepository.findById(mockId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Mock template not found")
        );

        MockBlueprint blueprint = mockAssemblyService.parseBlueprint(exam.getBlueprintJson());
        MockAssemblyService.AssembledMock assembled = mockAssemblyService.assembleMock(blueprint);

        int durationSeconds = (exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 180) * 60;
        List<String> questionIds = assembled.questions().stream().map(q -> q.getId().toString()).toList();
        String questionIdsJson;
        try {
            questionIdsJson = objectMapper.writeValueAsString(questionIds);
        } catch (Exception e) {
            questionIdsJson = "[]";
        }

        UUID currentUserId = currentUserProvider.getCurrentUserId();
        MockAttempt attempt = new MockAttempt();
        attempt.setUserId(currentUserId);
        attempt.setMockExam(exam);
        attempt.setStartedAt(Instant.now());
        attempt.setDurationSeconds(durationSeconds);
        attempt.setQuestionIdsJson(questionIdsJson);
        attempt.setStatus(MockAttemptStatus.IN_PROGRESS);

        MockAttempt savedAttempt = mockAttemptRepository.save(attempt);

        for (Question q : assembled.questions()) {
            AttemptAnswer answer = new AttemptAnswer();
            answer.setAttempt(savedAttempt);
            answer.setQuestion(q);
            answer.setState(AttemptAnswerState.NOT_VISITED);
            answer.setTimeSpentSeconds(0);
            answer.setMarksAwarded(0.0);
            attemptAnswerRepository.save(answer);
        }

        List<PracticeQuestionDto> safeQuestions = assembled.questions().stream().map(q -> new PracticeQuestionDto(
            q.getId(),
            q.getExternalId(),
            q.getType(),
            q.getMarks(),
            q.getStatement(),
            q.getOptionsJson(),
            q.getTopic() != null ? q.getTopic().getId() : null,
            q.getSubject() != null ? q.getSubject().getId() : null,
            q.getYear(),
            Collections.emptyList()
        )).toList();

        Instant now = Instant.now();
        return new StartMockAttemptResponse(
            savedAttempt.getId(),
            exam.getId(),
            exam.getTitle(),
            durationSeconds,
            assembled.sections(),
            safeQuestions,
            savedAttempt.getStartedAt(),
            now,
            assembled.shortfallWarnings()
        );
    }

    @Transactional
    public UpsertAnswerResponse upsertAnswer(UUID attemptId, UUID questionId, UpsertAnswerRequest req) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        MockAttempt attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found")
        );

        if (attempt.getStatus() != MockAttemptStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt is not in progress");
        }

        long dur = attempt.getDurationSeconds() != null ? attempt.getDurationSeconds().longValue() : 10800L;
        Instant deadline = attempt.getStartedAt().plusSeconds(dur + 60);
        if (Instant.now().isAfter(deadline)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt time has expired");
        }

        List<String> frozenIds;
        try {
            frozenIds = objectMapper.readValue(attempt.getQuestionIdsJson() != null ? attempt.getQuestionIdsJson() : "[]", new TypeReference<List<String>>() {});
        } catch (Exception e) {
            frozenIds = Collections.emptyList();
        }

        if (!frozenIds.contains(questionId.toString())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question " + questionId + " is not part of this attempt");
        }

        AttemptAnswerState parsedState = null;
        if (req.state() != null) {
            try {
                if ("ANSWERED_AND_MARKED".equals(req.state())) {
                    parsedState = AttemptAnswerState.ANSWERED_AND_MARKED_FOR_REVIEW;
                } else {
                    parsedState = AttemptAnswerState.valueOf(req.state());
                }
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid AttemptAnswerState: " + req.state());
            }
        }

        AttemptAnswer answer = attemptAnswerRepository.findByAttemptIdAndQuestionId(attemptId, questionId);
        if (answer == null) {
            Question q = questionRepository.findById(questionId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found")
            );
            answer = new AttemptAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(q);
        }

        if (req.selectedOptions() != null) {
            answer.setSelectedOptions(req.selectedOptions());
        }
        if (req.numericAnswer() != null) {
            answer.setNumericAnswer(req.numericAnswer());
        }
        if (parsedState != null) {
            answer.setState(parsedState);
        }
        if (req.timeSpentSeconds() != null && req.timeSpentSeconds() > 0) {
            answer.setTimeSpentSeconds(answer.getTimeSpentSeconds() + req.timeSpentSeconds());
        }

        AttemptAnswer savedAnswer = attemptAnswerRepository.save(answer);
        return new UpsertAnswerResponse(true, savedAnswer.getState() != null ? savedAnswer.getState().name() : "NOT_VISITED");
    }

    @Transactional
    public SubmitMockAttemptResponse submitAttempt(UUID attemptId, SubmitMockAttemptRequest req) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        MockAttempt attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found")
        );

        if (attempt.getStatus() != MockAttemptStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt already submitted or finalized");
        }

        long dur = attempt.getDurationSeconds() != null ? attempt.getDurationSeconds().longValue() : 10800L;
        Instant deadline = attempt.getStartedAt().plusSeconds(dur + 60);
        FinishedReason finishedReason = Instant.now().isAfter(deadline) ? FinishedReason.AUTO_SUBMITTED : FinishedReason.SUBMITTED;

        List<String> frozenIds;
        try {
            frozenIds = objectMapper.readValue(attempt.getQuestionIdsJson() != null ? attempt.getQuestionIdsJson() : "[]", new TypeReference<List<String>>() {});
        } catch (Exception e) {
            frozenIds = Collections.emptyList();
        }

        List<AttemptAnswer> answers = attemptAnswerRepository.findByAttemptId(attemptId);
        Map<UUID, AttemptAnswer> existingAnswers = answers.stream()
            .filter(a -> a.getQuestion() != null && a.getQuestion().getId() != null)
            .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a, b) -> a));

        double positiveScoreSum = 0.0;
        double negativeScoreSum = 0.0;
        int correctCount = 0;
        int wrongCount = 0;
        int unattemptedCount = 0;
        double maxMarks = 0.0;

        long startNano = System.nanoTime();
        for (String qidStr : frozenIds) {
            UUID qid = UUID.fromString(qidStr);
            Question question = questionRepository.findById(qid).orElse(null);
            if (question == null) continue;
            maxMarks += question.getMarks();

            AttemptAnswer answer = existingAnswers.get(qid);
            if (answer == null) {
                answer = new AttemptAnswer();
                answer.setAttempt(attempt);
                answer.setQuestion(question);
                answer.setState(AttemptAnswerState.NOT_VISITED);
            }

            GradingService.MockGradingResult grading = gradingService.gradeMockQuestion(
                question,
                answer.getSelectedOptions(),
                answer.getNumericAnswer(),
                answer.getState()
            );

            answer.setIsCorrect(grading.isCorrect());
            answer.setMarksAwarded(grading.marksAwarded());
            attemptAnswerRepository.save(answer);

            if (!grading.isAttempted()) {
                unattemptedCount++;
            } else if (Boolean.TRUE.equals(grading.isCorrect())) {
                correctCount++;
                positiveScoreSum += grading.marksAwarded();
            } else {
                wrongCount++;
                negativeScoreSum += grading.penalty();
                try {
                    cardSchedulingService.enrollPrerequisiteCards(currentUserId, qid);
                } catch (Exception e) {
                    logger.warn("Card auto-enrollment hook failure ignored for question {}: {}", qid, e.getMessage());
                }
            }
        }

        if (gateLmsMetrics != null) {
            gateLmsMetrics.recordGradingDuration("mock", Duration.ofNanos(System.nanoTime() - startNano));
        }

        double posScore = BigDecimal.valueOf(positiveScoreSum).setScale(2, RoundingMode.HALF_UP).doubleValue();
        double negScore = BigDecimal.valueOf(negativeScoreSum).setScale(2, RoundingMode.HALF_UP).doubleValue();
        double totScore = Math.max(0.0, BigDecimal.valueOf(positiveScoreSum - negativeScoreSum).setScale(2, RoundingMode.HALF_UP).doubleValue());

        attempt.setStatus(MockAttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        attempt.setPositiveScore(posScore);
        attempt.setNegativeScore(negScore);
        attempt.setTotalScore(totScore);
        attempt.setFinishedReason(finishedReason);

        mockAttemptRepository.save(attempt);

        if (gateLmsMetrics != null) {
            gateLmsMetrics.recordAttemptSubmitted("mock");
        }

        int durationSec = (int) ((attempt.getSubmittedAt() != null ? attempt.getSubmittedAt().getEpochSecond() : Instant.now().getEpochSecond()) - attempt.getStartedAt().getEpochSecond());
        int minutesMocked = Math.max(1, (durationSec + 59) / 60);

        if (rollupService != null) {
            rollupService.recordMockSubmit(
                LocalDate.now(),
                correctCount + wrongCount,
                correctCount,
                minutesMocked,
                currentUserId
            );
        }

        if (planProgressService != null) {
            try {
                Map<String, List<Question>> answeredByTopic = new HashMap<>();
                for (String qidStr : frozenIds) {
                    Question q = questionRepository.findById(UUID.fromString(qidStr)).orElse(null);
                    if (q != null && q.getTopic() != null) {
                        AttemptAnswer ans = existingAnswers.get(q.getId());
                        if (ans != null && (ans.getState() == AttemptAnswerState.ANSWERED ||
                            ans.getState() == AttemptAnswerState.ANSWERED_AND_MARKED ||
                            ans.getState() == AttemptAnswerState.ANSWERED_AND_MARKED_FOR_REVIEW)) {
                            answeredByTopic.computeIfAbsent(q.getTopic().getId(), k -> new ArrayList<>()).add(q);
                        }
                    }
                }

                int totalQuestionsAnswered = correctCount + wrongCount;
                for (Map.Entry<String, List<Question>> entry : answeredByTopic.entrySet()) {
                    String topicId = entry.getKey();
                    List<Question> qList = entry.getValue();
                    int topicMinutes = totalQuestionsAnswered > 0
                        ? Math.max(1, (minutesMocked * qList.size()) / totalQuestionsAnswered)
                        : 1;
                    planProgressService.onQuestionsAnswered(
                        LocalDate.now(),
                        topicId,
                        qList.size(),
                        topicMinutes,
                        currentUserId
                    );
                }
            } catch (Exception e) {
                logger.warn("Plan progress hook failure ignored during mock attempt submit: {}", e.getMessage(), e);
            }
        }

        if (achievementService != null) {
            try {
                achievementService.checkOnMockSubmitted(totScore, maxMarks, currentUserId);
            } catch (Exception e) {
                logger.warn("Achievement check failed in mock attempt submit: {}", e.getMessage(), e);
            }
        }

        return new SubmitMockAttemptResponse(
            attempt.getId(),
            attempt.getStatus().name(),
            finishedReason.name(),
            posScore,
            negScore,
            totScore,
            maxMarks,
            correctCount,
            wrongCount,
            unattemptedCount,
            frozenIds.size()
        );
    }

    @Transactional(readOnly = true)
    public MockResultResponse getResult(UUID attemptId) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        MockAttempt attempt = mockAttemptRepository.findByIdAndUserId(attemptId, currentUserId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Attempt not found")
        );

        if (attempt.getStatus() != MockAttemptStatus.SUBMITTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attempt has not been submitted yet");
        }

        MockExam exam = attempt.getMockExam();
        if (exam == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Mock template missing");
        }

        List<String> frozenIds;
        try {
            frozenIds = objectMapper.readValue(attempt.getQuestionIdsJson() != null ? attempt.getQuestionIdsJson() : "[]", new TypeReference<List<String>>() {});
        } catch (Exception e) {
            frozenIds = Collections.emptyList();
        }

        Map<UUID, AttemptAnswer> answersMap = attemptAnswerRepository.findByAttemptId(attemptId).stream()
            .filter(a -> a.getQuestion() != null && a.getQuestion().getId() != null)
            .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a, (a, b) -> a));

        List<MockQuestionReviewDto> questionReviews = new ArrayList<>();
        Map<String, List<Pair<Question, AttemptAnswer>>> subjectScoresMap = new LinkedHashMap<>();

        int totalTimeSpent = 0;
        double totalMaxMarks = 0.0;

        List<UUID> qUuids = frozenIds.stream().map(s -> {
            try { return UUID.fromString(s); } catch (Exception e) { return null; }
        }).filter(Objects::nonNull).toList();

        Map<UUID, List<String>> tagMap = questionConceptTagRepository != null
            ? questionConceptTagRepository.findSlugsGroupedByQuestionIds(qUuids)
            : Collections.emptyMap();

        for (String qidStr : frozenIds) {
            UUID qid = UUID.fromString(qidStr);
            Question question = questionRepository.findById(qid).orElse(null);
            if (question == null) continue;
            AttemptAnswer answer = answersMap.get(qid);

            totalMaxMarks += question.getMarks();
            if (answer != null) {
                totalTimeSpent += answer.getTimeSpentSeconds();
            }

            String subjectId = (question.getSubject() != null && question.getSubject().getId() != null)
                ? question.getSubject().getId() : "unknown";
            subjectScoresMap.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(new Pair<>(question, answer));

            questionReviews.add(new MockQuestionReviewDto(
                question.getId(),
                question.getExternalId() != null ? question.getExternalId() : "",
                question.getType().name(),
                question.getMarks(),
                question.getStatement(),
                question.getOptionsJson(),
                question.getSubject() != null ? question.getSubject().getId() : null,
                question.getSubject() != null ? question.getSubject().getName() : null,
                question.getTopic() != null ? question.getTopic().getId() : null,
                answer != null ? answer.getSelectedOptions() : null,
                answer != null ? answer.getNumericAnswer() : null,
                question.getCorrectOptions(),
                question.getNumericMin(),
                question.getNumericMax(),
                answer != null ? answer.getIsCorrect() : null,
                answer != null ? answer.getMarksAwarded() : 0.0,
                (answer != null && answer.getState() != null) ? answer.getState().name() : AttemptAnswerState.NOT_VISITED.name(),
                answer != null ? answer.getTimeSpentSeconds() : 0,
                question.getExplanation(),
                tagMap.getOrDefault(question.getId(), Collections.emptyList())
            ));
        }

        List<MockSectionScoreDto> sectionScores = new ArrayList<>();
        for (Map.Entry<String, List<Pair<Question, AttemptAnswer>>> entry : subjectScoresMap.entrySet()) {
            String subjId = entry.getKey();
            List<Pair<Question, AttemptAnswer>> pairs = entry.getValue();
            Subject subj = pairs.get(0).first().getSubject();
            int totalQuestions = pairs.size();

            int attemptedCount = (int) pairs.stream().filter(p -> p.second() != null &&
                p.second().getState() != AttemptAnswerState.NOT_VISITED &&
                p.second().getState() != AttemptAnswerState.NOT_ANSWERED).count();
            int correctCount = (int) pairs.stream().filter(p -> p.second() != null && Boolean.TRUE.equals(p.second().getIsCorrect())).count();
            int wrongCount = (int) pairs.stream().filter(p -> p.second() != null && Boolean.FALSE.equals(p.second().getIsCorrect())).count();
            int unattemptedCount = totalQuestions - attemptedCount;
            double marksAwarded = pairs.stream().mapToDouble(p -> p.second() != null ? p.second().getMarksAwarded() : 0.0).sum();
            double maxMarks = pairs.stream().mapToDouble(p -> p.first().getMarks().doubleValue()).sum();
            double accuracy = attemptedCount > 0 ? ((double) correctCount / (double) attemptedCount) * 100.0 : 0.0;
            int timeSpent = pairs.stream().mapToInt(p -> p.second() != null ? p.second().getTimeSpentSeconds() : 0).sum();

            sectionScores.add(new MockSectionScoreDto(
                subjId,
                subj != null ? subj.getName() : subjId,
                totalQuestions,
                attemptedCount,
                correctCount,
                wrongCount,
                unattemptedCount,
                BigDecimal.valueOf(marksAwarded).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                maxMarks,
                BigDecimal.valueOf(accuracy).setScale(1, RoundingMode.HALF_UP).doubleValue(),
                timeSpent
            ));
        }

        return new MockResultResponse(
            attempt.getId(),
            exam.getId(),
            exam.getTitle(),
            attempt.getStartedAt(),
            attempt.getSubmittedAt(),
            attempt.getDurationSeconds() != null ? attempt.getDurationSeconds() : 10800,
            attempt.getFinishedReason() != null ? attempt.getFinishedReason().name() : FinishedReason.SUBMITTED.name(),
            attempt.getTotalScore() != null ? attempt.getTotalScore() : 0.0,
            attempt.getPositiveScore() != null ? attempt.getPositiveScore() : 0.0,
            attempt.getNegativeScore() != null ? attempt.getNegativeScore() : 0.0,
            totalMaxMarks,
            totalTimeSpent,
            questionReviews,
            sectionScores
        );
    }

    @Transactional(readOnly = true)
    public List<MockAttemptHistoryDto> getAttemptHistory(UUID mockId) {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        return mockAttemptRepository.findByMockExamIdAndUserIdOrderByStartedAtDesc(mockId, currentUserId).stream().map(a ->
            new MockAttemptHistoryDto(
                a.getId(),
                a.getStartedAt(),
                a.getSubmittedAt(),
                a.getDurationSeconds(),
                a.getTotalScore(),
                a.getPositiveScore(),
                a.getNegativeScore(),
                a.getStatus() != null ? a.getStatus().name() : "IN_PROGRESS",
                a.getFinishedReason() != null ? a.getFinishedReason().name() : null
            )
        ).toList();
    }

    @Transactional(readOnly = true)
    public List<MockTrendDto> getMockTrend() {
        UUID currentUserId = currentUserProvider.getCurrentUserId();
        List<MockAttempt> attempts = mockAttemptRepository.findByUserIdAndStatusOrderBySubmittedAtAsc(currentUserId, MockAttemptStatus.SUBMITTED);
        return attempts.stream().map(a -> {
            MockExam exam = a.getMockExam();
            if (exam == null) return null;
            Instant subAt = a.getSubmittedAt() != null ? a.getSubmittedAt() : a.getStartedAt();
            double totalScore = a.getTotalScore() != null ? a.getTotalScore() : 0.0;
            double maxScore = exam.getTotalMarks() != null ? exam.getTotalMarks() : 100.0;
            double percentage = maxScore > 0 ? (totalScore / maxScore) * 100.0 : 0.0;

            return new MockTrendDto(
                a.getId(),
                exam.getId(),
                exam.getTitle(),
                subAt,
                totalScore,
                maxScore,
                BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP).doubleValue(),
                null
            );
        }).filter(Objects::nonNull).toList();
    }

    @Scheduled(fixedRate = 600000)
    @Transactional
    public void cleanupAbandonedAttempts() {
        Instant now = Instant.now();
        List<MockAttempt> inProgress = mockAttemptRepository.findByStatusOrderBySubmittedAtAsc(MockAttemptStatus.IN_PROGRESS);
        for (MockAttempt attempt : inProgress) {
            int timeoutSeconds = (attempt.getDurationSeconds() != null ? attempt.getDurationSeconds() : 10800) + 3600;
            Instant deadline = attempt.getStartedAt().plusSeconds(timeoutSeconds);
            if (now.isAfter(deadline)) {
                attempt.setStatus(MockAttemptStatus.ABANDONED);
                attempt.setFinishedReason(FinishedReason.ABANDONED);
                mockAttemptRepository.save(attempt);
            }
        }
    }

    public record Pair<A, B>(A first, B second) {}
}
