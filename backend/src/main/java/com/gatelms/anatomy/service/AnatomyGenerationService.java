package com.gatelms.anatomy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.ai.dto.AiChatRequest;
import com.gatelms.ai.dto.ChatMessageDto;
import com.gatelms.ai.dto.AiChatResponse;
import com.gatelms.ai.registry.ProviderRegistry;
import com.gatelms.ai.security.AiRateLimiter;
import com.gatelms.ai.service.AiChatService;
import com.gatelms.anatomy.dto.*;
import com.gatelms.anatomy.entity.*;
import com.gatelms.anatomy.exception.AnatomyExceptions.AnatomyLockedException;
import com.gatelms.anatomy.exception.AnatomyExceptions.AnatomyNotFoundException;
import com.gatelms.anatomy.repository.*;
import com.gatelms.entity.Question;
import com.gatelms.entity.QuestionProgress;
import com.gatelms.entity.QuestionProgressId;
import com.gatelms.repository.QuestionProgressRepository;
import com.gatelms.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;

@Service
public class AnatomyGenerationService {
    private static final Logger log = LoggerFactory.getLogger(AnatomyGenerationService.class);

    private final QuestionRepository questionRepository;
    private final QuestionProgressRepository questionProgressRepository;
    private final QuestionAnatomyRepository questionAnatomyRepository;
    private final ConceptTagRepository conceptTagRepository;
    private final QuestionConceptTagRepository questionConceptTagRepository;
    private final AnatomyValidationService anatomyValidationService;
    private final AiChatService aiChatService;
    private final AiRateLimiter aiRateLimiter;
    private final ObjectMapper objectMapper;
    private final ProviderRegistry providerRegistry;

    public AnatomyGenerationService(
        QuestionRepository questionRepository,
        QuestionProgressRepository questionProgressRepository,
        QuestionAnatomyRepository questionAnatomyRepository,
        ConceptTagRepository conceptTagRepository,
        QuestionConceptTagRepository questionConceptTagRepository,
        AnatomyValidationService anatomyValidationService,
        AiChatService aiChatService,
        AiRateLimiter aiRateLimiter,
        ObjectMapper objectMapper,
        ProviderRegistry providerRegistry
    ) {
        this.questionRepository = questionRepository;
        this.questionProgressRepository = questionProgressRepository;
        this.questionAnatomyRepository = questionAnatomyRepository;
        this.conceptTagRepository = conceptTagRepository;
        this.questionConceptTagRepository = questionConceptTagRepository;
        this.anatomyValidationService = anatomyValidationService;
        this.aiChatService = aiChatService;
        this.aiRateLimiter = aiRateLimiter;
        this.objectMapper = objectMapper;
        this.providerRegistry = providerRegistry;
    }

    public QuestionAnatomyDto getAnatomy(UUID questionId, UUID userId, boolean isAdmin) {
        if (!isAdmin) {
            Optional<QuestionProgress> progress = questionProgressRepository.findById(new QuestionProgressId(userId, questionId));
            boolean hasAttempted = progress.isPresent() && progress.get().getLastAttemptedAt() != null;
            if (!hasAttempted) {
                throw new AnatomyLockedException();
            }
        }

        QuestionAnatomy anatomy = questionAnatomyRepository.findByQuestionId(questionId)
            .orElseThrow(() -> new AnatomyNotFoundException("No anatomy dossier found for question " + questionId));

        if (anatomy.getStatus() == AnatomyStatus.REJECTED && !isAdmin) {
            throw new AnatomyNotFoundException("Anatomy dossier has been rejected");
        }

        return toDto(anatomy);
    }

    @Transactional
    public QuestionAnatomyDto generateOrGetCached(
        UUID questionId,
        UUID userId,
        String provider,
        String model,
        String authHeaderKey,
        boolean force
    ) {
        Optional<QuestionAnatomy> existing = questionAnatomyRepository.findByQuestionId(questionId);
        if (!force && existing.isPresent() && existing.get().getStatus() != AnatomyStatus.REJECTED &&
            existing.get().getParseError() == null && !"Generation parsing incomplete".equals(existing.get().getTestedConcept())
        ) {
            return toDto(existing.get());
        }

        aiRateLimiter.checkAnatomyGenerateLimit(userId);

        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new AnatomyNotFoundException("Question not found with ID: " + questionId));

        String userPrompt = buildUserPrompt(question);
        String effectiveProvider = providerRegistry.resolveStudyProvider(provider);
        AiChatRequest chatRequest = new AiChatRequest(
            List.of(new ChatMessageDto("user", userPrompt)),
            effectiveProvider,
            model,
            "study"
        );

        String rawResponse = "";
        AnatomyJsonPayload payload = null;
        String parseError = null;
        String lastUsedModel = null;

        try {
            AiChatResponse response1 = aiChatService.chat(chatRequest, authHeaderKey);
            lastUsedModel = response1.model();
            rawResponse = response1.reply();
            payload = anatomyValidationService.parseAndValidate(rawResponse);
        } catch (Exception e1) {
            log.warn("Anatomy generation first attempt failed for question {}: {}. Retrying once...", questionId, e1.getMessage());
            try {
                String retryPrompt = userPrompt + "\n\n[CORRECTION REQUIRED]: Your previous output failed validation with: " +
                    e1.getMessage() + ". Output strictly valid JSON without any markdown formatting or student-population stats.";
                AiChatRequest retryRequest = new AiChatRequest(
                    List.of(new ChatMessageDto("user", retryPrompt)),
                    effectiveProvider,
                    model,
                    "study"
                );
                AiChatResponse response2 = aiChatService.chat(retryRequest, authHeaderKey);
                lastUsedModel = response2.model();
                rawResponse = response2.reply();
                payload = anatomyValidationService.parseAndValidate(rawResponse);
            } catch (Exception e2) {
                log.error("Anatomy generation retry also failed for question {}: {}", questionId, e2.getMessage());
                parseError = e2.getMessage() != null ? e2.getMessage() : "Failed validation after retry";
            }
        }

        if (payload == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Question anatomy generation failed: " + (parseError != null ? parseError : "AI service unavailable")
            );
        }

        QuestionAnatomy anatomy = existing.orElseGet(() -> {
            QuestionAnatomy qa = new QuestionAnatomy();
            qa.setQuestion(question);
            return qa;
        });

        anatomy.setTestedConcept(payload.testedConcept());
        anatomy.setInvariantRule(payload.invariantRule());
        try {
            anatomy.setTrapAnalysisJson(objectMapper.writeValueAsString(payload.trapAnalysis()));
            anatomy.setPrerequisitesJson(objectMapper.writeValueAsString(payload.prerequisites()));
        } catch (Exception e) {
            anatomy.setTrapAnalysisJson("[]");
            anatomy.setPrerequisitesJson("[]");
        }
        anatomy.setSimilarPatternHint(payload.similarPatternHint());
        anatomy.setGroundingRefsJson("[]");
        anatomy.setStatus(AnatomyStatus.DRAFT);
        anatomy.setParseError(null);
        anatomy.setGeneratorModel(lastUsedModel);
        anatomy.setUpdatedAt(Instant.now());

        QuestionAnatomy saved = questionAnatomyRepository.save(anatomy);
        emitConceptTags(question, payload.conceptTags());

        return toDto(saved);
    }

    private void emitConceptTags(Question question, List<String> tags) {
        if (tags == null) return;
        var topic = question.getTopic();
        for (String tag : tags) {
            try {
                if (!conceptTagRepository.existsById(tag)) {
                    String[] parts = tag.split("-");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < parts.length; i++) {
                        if (i > 0) sb.append(" ");
                        if (!parts[i].isEmpty()) {
                            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
                        }
                    }
                    ConceptTag newTag = new ConceptTag(tag, sb.toString(), topic, null);
                    conceptTagRepository.save(newTag);
                }

                QuestionConceptTagId qctId = new QuestionConceptTagId(question.getId(), tag);
                if (!questionConceptTagRepository.existsById(qctId)) {
                    QuestionConceptTag link = new QuestionConceptTag(qctId, ConceptTagSource.ANATOMY, 1.0f);
                    questionConceptTagRepository.save(link);
                }
            } catch (Exception ex) {
                log.warn("Flywheel concept tag emission ignored error for tag '{}': {}", tag, ex.getMessage());
            }
        }
    }

    @Transactional
    public QuestionAnatomyDto softDeleteAnatomy(UUID questionId) {
        QuestionAnatomy anatomy = questionAnatomyRepository.findByQuestionId(questionId)
            .orElseThrow(() -> new AnatomyNotFoundException("Question anatomy not found for question " + questionId));
        anatomy.setStatus(AnatomyStatus.REJECTED);
        anatomy.setUpdatedAt(Instant.now());
        QuestionAnatomy saved = questionAnatomyRepository.save(anatomy);
        return toDto(saved);
    }

    @Transactional
    public QuestionAnatomyDto updateCuration(UUID id, CurationAnatomyUpdateRequest req) {
        QuestionAnatomy anatomy = questionAnatomyRepository.findById(id)
            .orElseThrow(() -> new AnatomyNotFoundException("Question anatomy not found with id " + id));

        if (req.status() != null) anatomy.setStatus(req.status());
        if (req.testedConcept() != null) anatomy.setTestedConcept(req.testedConcept());
        if (req.invariantRule() != null) anatomy.setInvariantRule(req.invariantRule());
        if (req.similarPatternHint() != null) anatomy.setSimilarPatternHint(req.similarPatternHint());
        anatomy.setUpdatedAt(Instant.now());

        QuestionAnatomy saved = questionAnatomyRepository.save(anatomy);
        return toDto(saved);
    }

    public Page<QuestionAnatomyDto> getCurationDrafts(Pageable pageable) {
        return questionAnatomyRepository.findByStatus(AnatomyStatus.DRAFT, pageable).map(this::toDto);
    }

    public QuestionAnatomyDto toDto(QuestionAnatomy anatomy) {
        List<TrapAnalysisItem> trapList;
        try {
            trapList = objectMapper.readValue(anatomy.getTrapAnalysisJson(), new TypeReference<List<TrapAnalysisItem>>() {});
        } catch (Exception e) {
            trapList = Collections.emptyList();
        }

        List<PrerequisiteItem> prereqList;
        try {
            prereqList = objectMapper.readValue(anatomy.getPrerequisitesJson(), new TypeReference<List<PrerequisiteItem>>() {});
        } catch (Exception e) {
            prereqList = Collections.emptyList();
        }

        return new QuestionAnatomyDto(
            anatomy.getId() != null ? anatomy.getId() : UUID.randomUUID(),
            anatomy.getQuestion() != null ? anatomy.getQuestion().getId() : UUID.randomUUID(),
            anatomy.getTestedConcept(),
            anatomy.getInvariantRule(),
            trapList,
            prereqList,
            anatomy.getSimilarPatternHint(),
            anatomy.getStatus(),
            anatomy.getParseError(),
            anatomy.getGeneratorModel(),
            anatomy.getGeneratedAt(),
            anatomy.getUpdatedAt()
        );
    }

    private String buildUserPrompt(Question q) {
        StringBuilder sb = new StringBuilder();
        sb.append("Deconstruct this question:\n");
        sb.append("Subject: ").append(q.getSubject() != null ? q.getSubject().getName() : "Unknown").append("\n");
        sb.append("Topic: ").append(q.getTopic() != null ? q.getTopic().getName() : "Unknown").append("\n");
        sb.append("Type: ").append(q.getType()).append("\n");
        sb.append("Marks: ").append(q.getMarks()).append("\n");
        sb.append("Statement:\n").append(q.getStatement()).append("\n");
        if (q.getOptionsJson() != null && !q.getOptionsJson().isBlank()) {
            sb.append("Options:\n").append(q.getOptionsJson()).append("\n");
        }
        if (q.getCorrectOptions() != null && !q.getCorrectOptions().isBlank()) {
            sb.append("Correct Answer: ").append(q.getCorrectOptions()).append("\n");
        }
        if (q.getNumericMin() != null || q.getNumericMax() != null) {
            sb.append("Numerical Range: [").append(q.getNumericMin()).append(" to ").append(q.getNumericMax()).append("]\n");
        }
        if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
            sb.append("Reference Explanation (Untrusted, may contain errors; verify independently):\n").append(q.getExplanation()).append("\n");
        }
        return sb.toString();
    }
}
