package com.gatelms.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.anatomy.repository.QuestionConceptTagRepository;
import com.gatelms.dto.QuestionDtos.*;
import com.gatelms.entity.*;
import com.gatelms.error.Exceptions.ConflictException;
import com.gatelms.error.Exceptions.EntityNotFoundException;
import com.gatelms.error.Exceptions.ForbiddenException;
import com.gatelms.error.Exceptions.ValidationException;
import com.gatelms.metrics.GateLmsMetrics;
import com.gatelms.repository.QuestionProgressRepository;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.repository.SubjectRepository;
import com.gatelms.repository.TopicRepository;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionProgressRepository questionProgressRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;
    private final GateLmsMetrics gateLmsMetrics;
    private final AchievementService achievementService;
    private final QuestionConceptTagRepository questionConceptTagRepository;

    public QuestionService(
        QuestionRepository questionRepository,
        QuestionProgressRepository questionProgressRepository,
        SubjectRepository subjectRepository,
        TopicRepository topicRepository,
        CurrentUserProvider currentUserProvider,
        ObjectMapper objectMapper,
        @Autowired(required = false) GateLmsMetrics gateLmsMetrics,
        @Autowired(required = false) AchievementService achievementService,
        @Autowired(required = false) QuestionConceptTagRepository questionConceptTagRepository
    ) {
        this.questionRepository = questionRepository;
        this.questionProgressRepository = questionProgressRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
        this.gateLmsMetrics = gateLmsMetrics;
        this.achievementService = achievementService;
        this.questionConceptTagRepository = questionConceptTagRepository;
    }

    @Transactional(readOnly = true)
    public Page<QuestionSummaryDto> searchQuestions(
        String subjectId,
        String topicId,
        String type,
        Integer year,
        String pass1Status,
        String pass2Status,
        Boolean bookmarked,
        String curationStatus,
        String source,
        String difficulty,
        String q,
        String sort,
        Pageable pageable
    ) {
        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim();
        String sortColumn;
        String s = sort != null ? sort.trim().toLowerCase() : "created_at";
        switch (s) {
            case "year" -> sortColumn = "year";
            case "marks" -> sortColumn = "marks";
            default -> sortColumn = "created_at";
        }

        Pageable unpagedSortPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        UUID userId = currentUserProvider.getCurrentUserId();
        Page<Question> page = questionRepository.findWithFilters(
            userId,
            (subjectId != null && !subjectId.isBlank()) ? subjectId : null,
            (topicId != null && !topicId.isBlank()) ? topicId : null,
            (type != null && !type.isBlank()) ? type : null,
            year,
            (pass1Status != null && !pass1Status.isBlank()) ? pass1Status : null,
            (pass2Status != null && !pass2Status.isBlank()) ? pass2Status : null,
            bookmarked,
            (curationStatus != null && !curationStatus.isBlank()) ? curationStatus : null,
            (source != null && !source.isBlank()) ? source : null,
            (difficulty != null && !difficulty.isBlank()) ? difficulty : null,
            normalizedQ,
            sortColumn,
            unpagedSortPageable
        );

        List<UUID> questionIds = page.getContent().stream().map(Question::getId).filter(Objects::nonNull).toList();
        Map<UUID, QuestionProgress> progressMap;
        if (!questionIds.isEmpty()) {
            progressMap = questionProgressRepository.findAllByUserIdAndQuestionIdIn(userId, questionIds).stream()
                .collect(Collectors.toMap(p -> p.getId().getQuestionId(), p -> p));
        } else {
            progressMap = Collections.emptyMap();
        }

        return page.map(question -> toSummaryDto(question, progressMap.get(question.getId())));
    }

    @Transactional(readOnly = true)
    public QuestionDto getQuestionById(UUID id) {
        Question question = questionRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("Question not found with id: " + id)
        );
        UUID userId = currentUserProvider.getCurrentUserId();
        QuestionProgress progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(userId, id).orElse(null);
        List<String> tags = questionConceptTagRepository != null ? questionConceptTagRepository.findSlugsByQuestionId(id) : Collections.emptyList();
        return toDto(question, progress, tags);
    }

    public QuestionDto createQuestion(QuestionUpsertDto dto) {
        QuestionValidationResultDto validation = validateUpsert(dto);
        if (!validation.valid()) {
            throw new ValidationException("Question validation failed", validation.fieldErrors());
        }

        Subject subject = subjectRepository.findById(dto.subjectId()).orElseThrow(() ->
            new EntityNotFoundException("Subject not found with id: " + dto.subjectId())
        );
        Topic topic = topicRepository.findById(dto.topicId()).orElseThrow(() ->
            new EntityNotFoundException("Topic not found with id: " + dto.topicId())
        );

        if (dto.externalId() != null && !dto.externalId().isBlank()) {
            if (questionRepository.findByExternalId(dto.externalId()) != null) {
                throw new ConflictException("Question with externalId '" + dto.externalId() + "' already exists");
            }
        }

        Question question = new Question();
        question.setExternalId(dto.externalId() != null && !dto.externalId().isBlank() ? dto.externalId() : null);
        question.setSubject(subject);
        question.setTopic(topic);
        question.setYear(dto.year());
        question.setType(dto.type());
        question.setMarks(dto.marks());
        question.setStatement(dto.statement().trim());
        question.setOptionsJson(dto.optionsJson());
        question.setCorrectOptions(dto.correctOptions() != null ? dto.correctOptions().trim() : null);
        question.setNumericMin(dto.numericMin());
        question.setNumericMax(dto.numericMax());
        question.setExplanation(dto.explanation() != null ? dto.explanation().trim() : null);
        question.setGateOverflowUrl(dto.gateOverflowUrl() != null ? dto.gateOverflowUrl().trim() : null);
        question.setSource(dto.source() != null ? dto.source() : "SAMPLE");
        question.setCurationStatus(dto.curationStatus() != null ? dto.curationStatus() : "UNREVIEWED");
        question.setCurationNote(dto.curationNote() != null ? dto.curationNote().trim() : null);
        question.setDifficulty(dto.difficulty() != null ? dto.difficulty().trim() : null);

        Question saved = questionRepository.save(question);
        return toDto(saved, null, null);
    }

    public QuestionDto updateQuestion(UUID id, QuestionUpsertDto dto) {
        QuestionValidationResultDto validation = validateUpsert(dto);
        if (!validation.valid()) {
            throw new ValidationException("Question validation failed", validation.fieldErrors());
        }

        Question question = questionRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("Question not found with id: " + id)
        );

        if (dto.version() != null && !Objects.equals(question.getVersion(), dto.version())) {
            throw new ConflictException("Question version mismatch: expected " + question.getVersion() + ", but got " + dto.version());
        }

        Subject subject = subjectRepository.findById(dto.subjectId()).orElseThrow(() ->
            new EntityNotFoundException("Subject not found with id: " + dto.subjectId())
        );
        Topic topic = topicRepository.findById(dto.topicId()).orElseThrow(() ->
            new EntityNotFoundException("Topic not found with id: " + dto.topicId())
        );

        question.setExternalId(dto.externalId() != null && !dto.externalId().isBlank() ? dto.externalId() : null);
        question.setSubject(subject);
        question.setTopic(topic);
        question.setYear(dto.year());
        question.setType(dto.type());
        question.setMarks(dto.marks());
        question.setStatement(dto.statement().trim());
        question.setOptionsJson(dto.optionsJson());
        question.setCorrectOptions(dto.correctOptions() != null ? dto.correctOptions().trim() : null);
        question.setNumericMin(dto.numericMin());
        question.setNumericMax(dto.numericMax());
        question.setExplanation(dto.explanation() != null ? dto.explanation().trim() : null);
        question.setGateOverflowUrl(dto.gateOverflowUrl() != null ? dto.gateOverflowUrl().trim() : null);
        if (dto.source() != null) question.setSource(dto.source());
        if (dto.curationStatus() != null) question.setCurationStatus(dto.curationStatus());
        if (dto.curationNote() != null) question.setCurationNote(dto.curationNote().trim());
        if (dto.difficulty() != null) question.setDifficulty(dto.difficulty().trim());
        question.setUpdatedAt(Instant.now());

        Question saved = questionRepository.saveAndFlush(question);
        UUID userId = currentUserProvider.getCurrentUserId();
        QuestionProgress progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(userId, id).orElse(null);
        return toDto(saved, progress, null);
    }

    public QuestionDto updateStatus(UUID id, QuestionStatusUpdateDto dto) {
        Question question = questionRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("Question not found with id: " + id)
        );

        UUID userId = currentUserProvider.getCurrentUserId();
        QuestionProgress progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(userId, id).orElseGet(() ->
            new QuestionProgress(new QuestionProgressId(userId, id))
        );

        if (dto.pass1Status() != null) progress.setPass1Status(dto.pass1Status());
        if (dto.pass2Status() != null) progress.setPass2Status(dto.pass2Status());
        if (dto.isBookmarked() != null) progress.setIsBookmarked(dto.isBookmarked());
        progress.setLastAttemptedAt(Instant.now());

        QuestionProgress savedProgress = questionProgressRepository.saveAndFlush(progress);
        return toDto(question, savedProgress, null);
    }

    public QuestionDto updateBookmark(UUID id, boolean bookmarked) {
        Question question = questionRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("Question not found with id: " + id)
        );

        UUID userId = currentUserProvider.getCurrentUserId();
        QuestionProgress progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(userId, id).orElseGet(() ->
            new QuestionProgress(new QuestionProgressId(userId, id))
        );

        progress.setIsBookmarked(bookmarked);
        progress.setBookmarkedAt(bookmarked ? Instant.now() : null);

        QuestionProgress savedProgress = questionProgressRepository.saveAndFlush(progress);
        return toDto(question, savedProgress, null);
    }

    public QuestionDto updateCuration(UUID id, CurationUpdateRequest dto) {
        User currentUser = currentUserProvider.getCurrentUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Only ADMIN users can curate questions");
        }

        Question question = questionRepository.findById(id).orElseThrow(() ->
            new EntityNotFoundException("Question not found with id: " + id)
        );

        Set<String> validStatuses = Set.of("ACCEPTED", "REJECTED", "UNREVIEWED");
        String status = dto.status() != null ? dto.status().trim().toUpperCase() : null;
        if (status == null || !validStatuses.contains(status)) {
            throw new ValidationException(
                "Invalid curation status",
                Map.of("curationStatus", "Must be one of ACCEPTED, REJECTED, UNREVIEWED")
            );
        }

        question.setCurationStatus(status);
        question.setCurationNote(dto.note() != null ? dto.note().trim() : null);
        question.setUpdatedAt(Instant.now());

        if ("ACCEPTED".equals(status) || "REJECTED".equals(status)) {
            if (gateLmsMetrics != null) {
                gateLmsMetrics.recordCurationDecision(status.toLowerCase());
            }
            if (achievementService != null) {
                try {
                    achievementService.checkOnCurationDecision();
                } catch (Exception e) {
                    // Ignore failure in achievement check
                }
            }
        }

        Question saved = questionRepository.saveAndFlush(question);
        QuestionProgress progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(currentUser.getId(), id).orElse(null);
        return toDto(saved, progress, null);
    }

    @Transactional(readOnly = true)
    public QuestionValidationResultDto validateUpsert(QuestionUpsertDto dto) {
        Map<String, String> errors = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        if (dto.statement() == null || dto.statement().isBlank()) {
            errors.put("statement", "Statement must not be blank");
        }
        if (dto.marks() != 1 && dto.marks() != 2) {
            errors.put("marks", "Marks must be 1 or 2");
        }
        if (dto.subjectId() == null || dto.subjectId().isBlank()) {
            errors.put("subjectId", "subjectId is required");
        }
        if (dto.topicId() == null || dto.topicId().isBlank()) {
            errors.put("topicId", "topicId is required");
        }

        if (dto.gateOverflowUrl() != null && !dto.gateOverflowUrl().isBlank()) {
            String url = dto.gateOverflowUrl().trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                errors.put("gateOverflowUrl", "Must be a valid http or https URL");
            }
        }

        if (dto.type() == QuestionType.MCQ) {
            validateOptions(dto.optionsJson(), errors, warnings, true);
            if (dto.correctOptions() == null || dto.correctOptions().isBlank()) {
                errors.put("correctOptions", "MCQ must have exactly 1 correct option");
            } else {
                List<String> opts = Arrays.stream(dto.correctOptions().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
                if (opts.size() != 1) {
                    errors.put("correctOptions", "MCQ must have exactly 1 correct option");
                }
            }
            if (dto.numericMin() != null || dto.numericMax() != null) {
                errors.put("numericMin", "MCQ questions must not have numericMin/numericMax");
            }
        } else if (dto.type() == QuestionType.MSQ) {
            validateOptions(dto.optionsJson(), errors, warnings, false);
            if (dto.correctOptions() == null || dto.correctOptions().isBlank()) {
                errors.put("correctOptions", "MSQ must have at least 1 correct option");
            } else {
                List<String> opts = Arrays.stream(dto.correctOptions().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
                if (opts.isEmpty() || opts.size() > 6) {
                    errors.put("correctOptions", "MSQ must have between 1 and 6 correct options");
                }
                Set<String> distinct = new HashSet<>(opts);
                if (distinct.size() != opts.size()) {
                    errors.put("correctOptions", "Duplicate options specified in correctOptions");
                }
            }
            if (dto.numericMin() != null || dto.numericMax() != null) {
                errors.put("numericMin", "MSQ questions must not have numericMin/numericMax");
            }
        } else if (dto.type() == QuestionType.NAT) {
            if (dto.numericMin() == null || dto.numericMax() == null) {
                errors.put("numericMin", "NAT questions require both numericMin and numericMax");
            } else if (dto.numericMin() > dto.numericMax()) {
                errors.put("numericMin", "numericMin must be less than or equal to numericMax");
            }
            if (dto.optionsJson() != null && !dto.optionsJson().isBlank()) {
                errors.put("optionsJson", "NAT questions must not have options");
            }
            if (dto.correctOptions() != null && !dto.correctOptions().isBlank()) {
                errors.put("correctOptions", "NAT questions must not have correctOptions");
            }
            if (dto.numericMin() != null && dto.numericMax() != null && (dto.numericMax() - dto.numericMin()) > 1000.0) {
                warnings.add("NAT tolerance span (" + (dto.numericMax() - dto.numericMin()) + ") > 1000 flagged as suspicious");
            }
        }

        if (dto.statement() != null && dto.statement().contains("$$")) {
            warnings.add("Display-math ($$...$$) detected in statement");
        } else {
            warnings.add("Display-math ($$...$$) absent in statement");
        }

        if (dto.explanation() == null || dto.explanation().isBlank()) {
            warnings.add("Explanation is empty");
        }

        if (dto.externalId() != null && !dto.externalId().isBlank()) {
            try {
                Question existing = questionRepository.findByExternalId(dto.externalId().trim());
                if (existing != null) {
                    warnings.add("External ID '" + dto.externalId() + "' already exists in question bank");
                }
            } catch (Exception ignored) {
            }
        }

        return new QuestionValidationResultDto(errors.isEmpty(), errors, warnings);
    }

    private void validateOptions(
        String optionsJson,
        Map<String, String> errors,
        List<String> warnings,
        boolean isMcq
    ) {
        if (optionsJson == null || optionsJson.isBlank()) {
            errors.put("optionsJson", "Options must be provided");
            return;
        }
        try {
            List<Map<String, Object>> list = objectMapper.readValue(optionsJson, new TypeReference<List<Map<String, Object>>>() {});
            if (list.size() < 2) {
                errors.put("optionsJson", "At least 2 options are required, found " + list.size());
            } else if (list.size() > 6) {
                errors.put("optionsJson", "At most 6 options are allowed, found " + list.size());
            } else if (isMcq && list.size() < 4) {
                warnings.add("Options count (" + list.size() + ") < 4 for MCQ");
            }
        } catch (Exception ex) {
            errors.put("optionsJson", "Invalid options JSON structure: " + ex.getMessage());
        }
    }

    public QuestionSummaryDto toSummaryDto(Question q, QuestionProgress progress) {
        return new QuestionSummaryDto(
            q.getId(),
            q.getExternalId(),
            q.getSubject() != null ? q.getSubject().getId() : null,
            q.getSubject() != null ? q.getSubject().getName() : null,
            q.getTopic() != null ? q.getTopic().getId() : null,
            q.getTopic() != null ? q.getTopic().getName() : null,
            q.getYear(),
            q.getType(),
            q.getMarks(),
            q.getStatement(),
            progress != null && progress.getPass1Status() != null ? progress.getPass1Status() : Pass1Status.PENDING,
            progress != null && progress.getPass2Status() != null ? progress.getPass2Status() : Pass2Status.PENDING,
            progress != null && progress.getIsBookmarked() != null ? progress.getIsBookmarked() : false,
            progress != null ? progress.getBookmarkedAt() : null,
            progress != null ? progress.getLastAttemptedAt() : null,
            q.getSource(),
            q.getCurationStatus(),
            q.getCurationNote(),
            q.getDifficulty(),
            q.getCreatedAt()
        );
    }

    public QuestionDto toDto(Question q, QuestionProgress progress, List<String> conceptTags) {
        List<String> resolvedTags = conceptTags != null ? conceptTags
            : (questionConceptTagRepository != null && q.getId() != null ? questionConceptTagRepository.findSlugsByQuestionId(q.getId()) : Collections.emptyList());

        return new QuestionDto(
            q.getId(),
            q.getExternalId(),
            q.getSubject() != null ? q.getSubject().getId() : null,
            q.getSubject() != null ? q.getSubject().getName() : null,
            q.getTopic() != null ? q.getTopic().getId() : null,
            q.getTopic() != null ? q.getTopic().getName() : null,
            q.getYear(),
            q.getType(),
            q.getMarks(),
            q.getStatement(),
            q.getOptionsJson(),
            q.getCorrectOptions(),
            q.getNumericMin(),
            q.getNumericMax(),
            q.getExplanation(),
            q.getGateOverflowUrl(),
            progress != null && progress.getPass1Status() != null ? progress.getPass1Status() : Pass1Status.PENDING,
            progress != null && progress.getPass2Status() != null ? progress.getPass2Status() : Pass2Status.PENDING,
            progress != null && progress.getIsBookmarked() != null ? progress.getIsBookmarked() : false,
            progress != null ? progress.getBookmarkedAt() : null,
            progress != null ? progress.getLastAttemptedAt() : null,
            q.getSource(),
            q.getCurationStatus(),
            q.getCurationNote(),
            q.getDifficulty(),
            q.getCreatedAt(),
            q.getUpdatedAt(),
            q.getVersion(),
            resolvedTags
        );
    }
}
