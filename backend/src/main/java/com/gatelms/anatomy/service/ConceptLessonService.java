package com.gatelms.anatomy.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatelms.ai.config.AiProperties;
import com.gatelms.ai.dto.AiDtos.AiChatRequest;
import com.gatelms.ai.dto.AiDtos.ChatMessageDto;
import com.gatelms.ai.dto.AiDtos.AiChatResponse;
import com.gatelms.ai.registry.ProviderRegistry;
import com.gatelms.ai.security.AiRateLimiter;
import com.gatelms.ai.service.AiChatService;
import com.gatelms.anatomy.dto.AnatomyDtos.*;
import com.gatelms.anatomy.entity.ConceptTag;
import com.gatelms.anatomy.entity.ConceptUnit;
import com.gatelms.anatomy.entity.ConceptUnitStatus;
import com.gatelms.anatomy.exception.AnatomyExceptions.AnatomyValidationException;
import com.gatelms.anatomy.repository.ConceptTagRepository;
import com.gatelms.anatomy.repository.ConceptUnitRepository;
import com.gatelms.anatomy.repository.QuestionConceptTagRepository;
import com.gatelms.anatomy.repository.TopicPrimerRepository;
import com.gatelms.entity.Question;
import com.gatelms.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ConceptLessonService {
    private static final Logger log = LoggerFactory.getLogger(ConceptLessonService.class);

    private final ConceptTagRepository conceptTagRepository;
    private final ConceptUnitRepository conceptUnitRepository;
    private final QuestionConceptTagRepository questionConceptTagRepository;
    private final QuestionRepository questionRepository;
    private final TopicPrimerRepository topicPrimerRepository;
    private final AiChatService aiChatService;
    private final AiRateLimiter aiRateLimiter;
    private final TopicGroundingSampler topicGroundingSampler;
    private final ProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    private static final Pattern POPULATION_STAT_PATTERN = Pattern.compile(
        "(?i)(\\d+\\s*(%|percent)\\s+(of\\s+)?(students|candidates|aspirants|test-takers|examinees|cohort|learners)|" +
        "\\b(majority|minority|most|many)\\s+of\\s+(students|candidates|aspirants|test-takers|examinees)\\b|" +
        "\\b(students|candidates|aspirants|test-takers|examinees)\\s+[^.\\n]{0,60}\\b\\d+\\s*(%|percent))"
    );

    public ConceptLessonService(
        ConceptTagRepository conceptTagRepository,
        ConceptUnitRepository conceptUnitRepository,
        QuestionConceptTagRepository questionConceptTagRepository,
        QuestionRepository questionRepository,
        TopicPrimerRepository topicPrimerRepository,
        AiChatService aiChatService,
        AiRateLimiter aiRateLimiter,
        AiProperties aiProperties,
        TopicGroundingSampler topicGroundingSampler,
        ProviderRegistry providerRegistry
    ) {
        this.conceptTagRepository = conceptTagRepository;
        this.conceptUnitRepository = conceptUnitRepository;
        this.questionConceptTagRepository = questionConceptTagRepository;
        this.questionRepository = questionRepository;
        this.topicPrimerRepository = topicPrimerRepository;
        this.aiChatService = aiChatService;
        this.aiRateLimiter = aiRateLimiter;
        this.topicGroundingSampler = topicGroundingSampler != null ? topicGroundingSampler : new TopicGroundingSampler();
        this.providerRegistry = providerRegistry != null ? providerRegistry : new ProviderRegistry(aiProperties);
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    @Transactional(readOnly = true)
    public ConceptLessonResponseDto getLesson(String slug) {
        ConceptTag tag = findConceptTag(slug);
        Optional<ConceptUnit> existing = conceptUnitRepository.findByConceptTagSlug(tag.getSlug());
        ConceptLessonDto lessonDto = (existing.isPresent() && existing.get().getStatus() != ConceptUnitStatus.REJECTED)
            ? toLessonDto(existing.get(), tag)
            : null;

        List<ConceptMappedQuestionItemDto> mappedQuestionItems = getMappedQuestionItems(tag.getSlug());

        return new ConceptLessonResponseDto(
            lessonDto,
            tag.getSlug(),
            tag.getLabel(),
            tag.getTopic() != null ? tag.getTopic().getId() : "",
            tag.getTopic() != null ? tag.getTopic().getName() : "",
            mappedQuestionItems.size(),
            mappedQuestionItems
        );
    }

    @Transactional
    public ConceptLessonResponseDto generateOrGetCached(
        String slug,
        UUID userId,
        String provider,
        String model,
        String authHeaderKey,
        boolean force
    ) {
        ConceptTag tag = findConceptTag(slug);
        Optional<ConceptUnit> existing = conceptUnitRepository.findByConceptTagSlug(tag.getSlug());

        if (!force && existing.isPresent() && existing.get().getStatus() != ConceptUnitStatus.REJECTED && existing.get().getParseError() == null) {
            List<ConceptMappedQuestionItemDto> mappedQuestionItems = getMappedQuestionItems(tag.getSlug());
            return new ConceptLessonResponseDto(
                toLessonDto(existing.get(), tag),
                tag.getSlug(),
                tag.getLabel(),
                tag.getTopic() != null ? tag.getTopic().getId() : "",
                tag.getTopic() != null ? tag.getTopic().getName() : "",
                mappedQuestionItems.size(),
                mappedQuestionItems
            );
        }

        aiRateLimiter.checkLessonGenerateLimit(userId);

        var links = questionConceptTagRepository.findByIdConceptTagSlug(slug, Pageable.unpaged()).getContent();
        if (links.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot generate concept lesson: Concept tag '" + slug + "' has 0 mapped questions."
            );
        }

        List<UUID> questionIds = links.stream().map(l -> l.getId().getQuestionId()).toList();
        List<Question> allMappedQuestions = questionRepository.findAllById(questionIds).stream()
            .filter(q -> !"REJECTED".equals(q.getCurationStatus()))
            .toList();

        if (allMappedQuestions.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot generate concept lesson: All mapped questions for '" + slug + "' are rejected."
            );
        }

        List<Question> sampleQuestions = topicGroundingSampler.stratifySample(allMappedQuestions, 15);
        Set<String> validExternalIds = allMappedQuestions.stream()
            .map(Question::getExternalId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        String topicPrimerCoreIdea = null;
        if (tag.getTopic() != null && tag.getTopic().getId() != null) {
            topicPrimerCoreIdea = topicPrimerRepository.findByTopicId(tag.getTopic().getId())
                .map(com.gatelms.anatomy.entity.TopicPrimer::getCoreIdea)
                .orElse(null);
        }

        String prompt = buildConceptLessonPrompt(tag, sampleQuestions, topicPrimerCoreIdea);
        String effectiveProvider = providerRegistry.resolveStudyProvider(provider);
        AiChatRequest chatRequest = new AiChatRequest(
            List.of(new ChatMessageDto("user", prompt)),
            effectiveProvider,
            model,
            "study"
        );

        ConceptLessonPayload payload = null;
        String parseError = null;
        String lastUsedModel = null;
        int attempts = 0;
        int maxAttempts = 2;

        while (attempts < maxAttempts && payload == null) {
            attempts++;
            try {
                AiChatResponse response = aiChatService.chat(chatRequest, authHeaderKey);
                lastUsedModel = response.model();
                payload = parseAndValidateLessonPayload(response.reply(), validExternalIds);
                parseError = null;
            } catch (Exception e) {
                log.warn("Concept lesson generation attempt {} failed for '{}': {}", attempts, slug, e.getMessage());
                parseError = e.getMessage() != null ? e.getMessage() : "Failed concept lesson generation";
            }
        }

        if (payload == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Concept lesson generation failed: " + (parseError != null ? parseError : "AI service unavailable")
            );
        }

        ConceptUnit unit = existing.orElseGet(() -> {
            ConceptUnit cu = new ConceptUnit();
            cu.setConceptTag(tag);
            return cu;
        });

        unit.setTitle(payload.title() != null && !payload.title().isBlank() ? payload.title() : tag.getLabel());
        unit.setMotivation(payload.motivation());
        unit.setCoreIdea(payload.coreIdea());
        unit.setInvariant(payload.invariant());
        unit.setWorkedExample(payload.workedExample());
        unit.setWorkedExampleExternalId(payload.workedExampleExternalId());
        unit.setTraps(payload.traps());
        unit.setStatus(ConceptUnitStatus.DRAFT);
        unit.setParseError(null);
        unit.setGeneratorModel(lastUsedModel);

        ConceptUnit saved = conceptUnitRepository.save(unit);
        List<ConceptMappedQuestionItemDto> mappedQuestionItems = getMappedQuestionItems(tag.getSlug());

        return new ConceptLessonResponseDto(
            toLessonDto(saved, tag),
            tag.getSlug(),
            tag.getLabel(),
            tag.getTopic() != null ? tag.getTopic().getId() : "",
            tag.getTopic() != null ? tag.getTopic().getName() : "",
            mappedQuestionItems.size(),
            mappedQuestionItems
        );
    }

    @Transactional
    public ConceptLessonDto updateLesson(String slug, CurationLessonUpdateRequest req) {
        ConceptTag tag = findConceptTag(slug);
        ConceptUnit unit = conceptUnitRepository.findByConceptTagSlug(tag.getSlug())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept lesson not found for slug: " + slug));

        if (req.status() != null) unit.setStatus(req.status());
        if (req.title() != null) unit.setTitle(req.title());
        if (req.motivation() != null) unit.setMotivation(req.motivation());
        if (req.coreIdea() != null) unit.setCoreIdea(req.coreIdea());
        if (req.invariant() != null) unit.setInvariant(req.invariant());
        if (req.workedExample() != null) unit.setWorkedExample(req.workedExample());
        if (req.workedExampleExternalId() != null) unit.setWorkedExampleExternalId(req.workedExampleExternalId());
        if (req.traps() != null) unit.setTraps(req.traps());
        unit.setParseError(null);

        ConceptUnit saved = conceptUnitRepository.save(unit);
        return toLessonDto(saved, tag);
    }

    @Transactional
    public void deleteLesson(String slug) {
        ConceptTag tag = findConceptTag(slug);
        conceptUnitRepository.findByConceptTagSlug(tag.getSlug()).ifPresent(conceptUnitRepository::delete);
    }

    @Transactional(readOnly = true)
    public Page<ConceptLessonDto> getCurationLessons(Pageable pageable) {
        Map<String, ConceptTag> tagsMap = conceptTagRepository.findAll().stream()
            .collect(Collectors.toMap(ConceptTag::getSlug, t -> t, (a, b) -> a));

        return conceptUnitRepository.findAll(pageable).map(cu -> {
            String slug = cu.getConceptTag() != null ? cu.getConceptTag().getSlug() : "";
            ConceptTag tag = tagsMap.getOrDefault(slug, new ConceptTag(slug, cu.getTitle(), null, null));
            return toLessonDto(cu, tag);
        });
    }

    public String buildConceptLessonPrompt(
        ConceptTag tag,
        List<Question> sampleQuestions,
        String topicPrimerCoreIdea
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert computer science professor designing a rigorous, canonical Concept Lesson for GATE CSE preparation.\n");
        sb.append("Target Concept: ").append(tag.getLabel()).append(" (slug: ").append(tag.getSlug()).append(")\n");
        sb.append("Topic: ").append(tag.getTopic() != null ? tag.getTopic().getName() : "Unknown").append("\n");
        sb.append("Subject: ").append((tag.getTopic() != null && tag.getTopic().getSubject() != null) ? tag.getTopic().getSubject().getName() : "Unknown").append("\n");

        if (topicPrimerCoreIdea != null && !topicPrimerCoreIdea.isBlank()) {
            sb.append("\nTopic Orientation Context (Background Core Idea):\n").append(topicPrimerCoreIdea.trim()).append("\n");
        }

        if (sampleQuestions != null && !sampleQuestions.isEmpty()) {
            sb.append("\nAvailable Mapped Questions for this Concept (").append(sampleQuestions.size()).append(" stratified samples):\n");
            for (int i = 0; i < sampleQuestions.size(); i++) {
                sb.append(topicGroundingSampler.formatQuestionSnippet(i + 1, sampleQuestions.get(i), 1500)).append("\n");
            }
        }

        sb.append("\nGenerate a complete Concept Lesson matching this JSON schema:\n")
          .append("{\n")
          .append("  \"title\": \"").append(tag.getLabel()).append("\",\n")
          .append("  \"motivation\": \"Plain-language intuition explaining why this concept exists, the concurrency or systems problem it solves, and its role in GATE CSE\",\n")
          .append("  \"core_idea\": \"The central mathematical, invariant, or algorithmic formalisms and operational definitions with KaTeX formulas ($...$)\",\n")
          .append("  \"invariant\": \"The fundamental logical invariant or rule that must hold across all execution interleavings\",\n")
          .append("  \"worked_example\": \"Mandatory step-by-step walkthrough of ONE canonical problem adapted from the mapped question list above (detailed numbered steps 1., 2., 3. with concrete states/numbers)\",\n")
          .append("  \"worked_example_external_id\": \"The EXACT externalId of the mapped question adapted for the worked example (must be one of the IDs provided in brackets above, e.g. 'SO-OS-0001')\",\n")
          .append("  \"traps\": \"2 to 4 critical examiner traps, subtle race/deadlock edge cases, or common false assumptions\"\n")
          .append("}\n\n")
          .append("Important Rules (Binding PC-2 and AF-1 Contract):\n")
          .append("1. 'worked_example_external_id' MUST be chosen from the provided mapped question list and match its externalId character-for-character.\n")
          .append("2. The entire lesson content ('motivation' + 'core_idea' + 'invariant' + 'worked_example' + 'traps') must be between 300 and 600 words total.\n")
          .append("3. 'worked_example' must be a detailed, concrete calculation/derivation walkthrough with numbered steps.\n")
          .append("4. Anti-fabrication (AF-1): Do NOT invent student statistics, exam pass rates, or population percentages (e.g., do NOT write '70% of students fail this').\n")
          .append("5. KaTeX formulas ($...$) are supported for mathematical and logic notation.\n")
          .append("6. Return ONLY valid JSON.\n");

        return sb.toString();
    }

    public ConceptLessonPayload parseAndValidateLessonPayload(String rawResponse, Set<String> validExternalIds) {
        String jsonText = extractJson(rawResponse);
        ConceptLessonPayload payload;
        try {
            payload = objectMapper.readValue(jsonText, ConceptLessonPayload.class);
        } catch (Exception e) {
            throw new AnatomyValidationException("Malformed or unparseable JSON: " + e.getMessage());
        }

        validateLessonPayload(payload, validExternalIds);
        return payload;
    }

    private void validateLessonPayload(ConceptLessonPayload payload, Set<String> validExternalIds) {
        List<String> errors = new ArrayList<>();

        if (payload.motivation() == null || payload.motivation().isBlank()) {
            errors.add("motivation must not be blank");
        }
        if (payload.coreIdea() == null || payload.coreIdea().isBlank()) {
            errors.add("core_idea must not be blank");
        }
        if (payload.workedExample() == null || payload.workedExample().isBlank()) {
            errors.add("worked_example must not be blank");
        }

        String extId = payload.workedExampleExternalId() != null ? payload.workedExampleExternalId().trim() : "";
        if (extId.isEmpty()) {
            errors.add("PC-2 violation: worked_example_external_id must not be blank and must cite a mapped question");
        } else if (validExternalIds != null && !validExternalIds.isEmpty() && !validExternalIds.contains(extId)) {
            errors.add("PC-2 violation: cited worked_example_external_id '" + extId + "' is not mapped to this concept");
        }

        int totalWords = countWords(
            payload.motivation(),
            payload.coreIdea(),
            payload.invariant() != null ? payload.invariant() : "",
            payload.workedExample(),
            payload.traps() != null ? payload.traps() : ""
        );
        if (totalWords < 300 || totalWords > 600) {
            errors.add("PC-2 violation: total lesson word count must be between 300 and 600 words (actual: " + totalWords + " words)");
        }

        StringBuilder allText = new StringBuilder();
        if (payload.motivation() != null) allText.append(payload.motivation()).append(" ");
        if (payload.coreIdea() != null) allText.append(payload.coreIdea()).append(" ");
        if (payload.invariant() != null) allText.append(payload.invariant()).append(" ");
        if (payload.workedExample() != null) allText.append(payload.workedExample()).append(" ");
        if (payload.traps() != null) allText.append(payload.traps()).append(" ");

        if (POPULATION_STAT_PATTERN.matcher(allText.toString()).find()) {
            errors.add("AF-1 violation: lesson contains fabricated student-population statistics or percentage claims");
        }

        if (!errors.isEmpty()) {
            throw new AnatomyValidationException(String.join("; ", errors));
        }
    }

    private int countWords(String... texts) {
        int count = 0;
        for (String t : texts) {
            if (t == null) continue;
            String trimmed = t.trim();
            if (trimmed.isEmpty()) continue;
            count += trimmed.split("\\s+").length;
        }
        return count;
    }

    private String extractJson(String raw) {
        String trimmed = raw.trim();
        String candidate;
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            candidate = trimmed;
        } else {
            var matcher = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```", Pattern.CASE_INSENSITIVE).matcher(trimmed);
            if (matcher.find()) {
                candidate = matcher.group(1).trim();
            } else {
                int firstBrace = trimmed.indexOf('{');
                int lastBrace = trimmed.lastIndexOf('}');
                if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                    candidate = trimmed.substring(firstBrace, lastBrace + 1);
                } else {
                    candidate = trimmed;
                }
            }
        }
        return AnatomyValidationService.repairJsonEscapes(candidate);
    }

    private ConceptTag findConceptTag(String slug) {
        return conceptTagRepository.findById(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Concept tag not found: " + slug));
    }

    private List<ConceptMappedQuestionItemDto> getMappedQuestionItems(String slug) {
        var links = questionConceptTagRepository.findByIdConceptTagSlug(slug, Pageable.unpaged()).getContent();
        if (links.isEmpty()) return Collections.emptyList();
        List<UUID> questionIds = links.stream().map(l -> l.getId().getQuestionId()).toList();
        List<Question> questions = questionRepository.findAllById(questionIds).stream()
            .filter(q -> !"REJECTED".equals(q.getCurationStatus()))
            .toList();

        return questions.stream().map(q -> new ConceptMappedQuestionItemDto(
            q.getId(),
            q.getExternalId() != null ? q.getExternalId() : "",
            q.getStatement(),
            q.getType() != null ? q.getType().name() : "",
            q.getDifficulty(),
            q.getYear()
        )).toList();
    }

    private ConceptLessonDto toLessonDto(ConceptUnit unit, ConceptTag tag) {
        return new ConceptLessonDto(
            unit.getId() != null ? unit.getId() : UUID.randomUUID(),
            tag.getSlug(),
            tag.getLabel(),
            (unit.getTitle() != null && !unit.getTitle().isBlank()) ? unit.getTitle() : tag.getLabel(),
            unit.getMotivation(),
            unit.getCoreIdea(),
            unit.getInvariant(),
            unit.getWorkedExample(),
            unit.getWorkedExampleExternalId(),
            unit.getTraps(),
            unit.getStatus() != null ? unit.getStatus().name() : "DRAFT",
            unit.getParseError(),
            unit.getGeneratorModel(),
            unit.getGeneratedAt()
        );
    }
}
