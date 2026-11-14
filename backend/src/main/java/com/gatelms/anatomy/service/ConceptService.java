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
import com.gatelms.anatomy.entity.*;
import com.gatelms.anatomy.exception.AnatomyExceptions.AnatomyNotFoundException;
import com.gatelms.anatomy.exception.AnatomyExceptions.NoSourceQuestionsException;
import com.gatelms.anatomy.repository.*;
import com.gatelms.dto.QuestionDtos.QuestionSummaryDto;
import com.gatelms.entity.Pass1Status;
import com.gatelms.entity.Pass2Status;
import com.gatelms.entity.Question;
import com.gatelms.entity.QuestionProgress;
import com.gatelms.entity.QuestionProgressId;
import com.gatelms.repository.QuestionProgressRepository;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ConceptService {
    private static final Logger log = LoggerFactory.getLogger(ConceptService.class);

    private final ConceptTagRepository conceptTagRepository;
    private final QuestionConceptTagRepository questionConceptTagRepository;
    private final QuestionAnatomyRepository questionAnatomyRepository;
    private final ConceptUnitRepository conceptUnitRepository;
    private final RetrievalCardRepository retrievalCardRepository;
    private final QuestionRepository questionRepository;
    private final QuestionProgressRepository questionProgressRepository;
    private final CurrentUserProvider currentUserProvider;
    private final AiChatService aiChatService;
    private final AiRateLimiter aiRateLimiter;
    private final AnatomyValidationService anatomyValidationService;
    private final ProviderRegistry providerRegistry;
    private final ObjectMapper objectMapper;

    public ConceptService(
        ConceptTagRepository conceptTagRepository,
        QuestionConceptTagRepository questionConceptTagRepository,
        QuestionAnatomyRepository questionAnatomyRepository,
        ConceptUnitRepository conceptUnitRepository,
        RetrievalCardRepository retrievalCardRepository,
        QuestionRepository questionRepository,
        QuestionProgressRepository questionProgressRepository,
        CurrentUserProvider currentUserProvider,
        AiChatService aiChatService,
        AiRateLimiter aiRateLimiter,
        AnatomyValidationService anatomyValidationService,
        ProviderRegistry providerRegistry,
        ObjectMapper objectMapper
    ) {
        this.conceptTagRepository = conceptTagRepository;
        this.questionConceptTagRepository = questionConceptTagRepository;
        this.questionAnatomyRepository = questionAnatomyRepository;
        this.conceptUnitRepository = conceptUnitRepository;
        this.retrievalCardRepository = retrievalCardRepository;
        this.questionRepository = questionRepository;
        this.questionProgressRepository = questionProgressRepository;
        this.currentUserProvider = currentUserProvider;
        this.aiChatService = aiChatService;
        this.aiRateLimiter = aiRateLimiter;
        this.anatomyValidationService = anatomyValidationService;
        this.providerRegistry = providerRegistry != null ? providerRegistry : new ProviderRegistry(new AiProperties());
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    public ConceptUnitDto getConcept(String slug) {
        ConceptUnit unit = conceptUnitRepository.findByConceptTagSlug(slug)
            .orElseThrow(() -> new AnatomyNotFoundException("Concept unit not found for slug '" + slug + "'"));

        List<RetrievalCardDto> cards = retrievalCardRepository.findByConceptUnitIdOrderByCardOrderAsc(unit.getId())
            .stream().map(this::toDto).toList();

        return toDto(unit, cards);
    }

    @Transactional(readOnly = true)
    public Page<QuestionSummaryDto> getConceptQuestions(String slug, Pageable pageable) {
        Page<QuestionConceptTag> tagLinksPage = questionConceptTagRepository.findByIdConceptTagSlug(slug, pageable);
        List<UUID> questionIds = tagLinksPage.getContent().stream().map(l -> l.getId().getQuestionId()).toList();
        if (questionIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, tagLinksPage.getTotalElements());
        }

        Map<UUID, Question> questions = questionRepository.findAllById(questionIds).stream()
            .collect(Collectors.toMap(Question::getId, q -> q));

        UUID userId = currentUserProvider.getCurrentUserId();
        List<QuestionProgressId> progressIds = questionIds.stream().map(id -> new QuestionProgressId(userId, id)).toList();
        Map<UUID, QuestionProgress> progresses = questionProgressRepository.findAllById(progressIds).stream()
            .collect(Collectors.toMap(p -> p.getId().getQuestionId(), p -> p));

        List<QuestionSummaryDto> summaries = questionIds.stream().map(qId -> {
            Question q = questions.get(qId);
            return toSummary(q, progresses.get(qId));
        }).filter(Objects::nonNull).toList();

        return new PageImpl<>(summaries, pageable, tagLinksPage.getTotalElements());
    }

    @Transactional
    public ConceptUnitDto generateOrGetCached(
        String slug,
        UUID userId,
        String provider,
        String model,
        String authHeaderKey
    ) {
        Optional<ConceptUnit> existing = conceptUnitRepository.findByConceptTagSlug(slug);
        if (existing.isPresent() && existing.get().getStatus() != ConceptUnitStatus.REJECTED && existing.get().getParseError() == null) {
            List<RetrievalCardDto> cards = retrievalCardRepository.findByConceptUnitIdOrderByCardOrderAsc(existing.get().getId())
                .stream().map(this::toDto).toList();
            return toDto(existing.get(), cards);
        }

        aiRateLimiter.checkConceptGenerateLimit(userId);

        ConceptTag tag = conceptTagRepository.findById(slug)
            .orElseThrow(() -> new AnatomyNotFoundException("Concept tag not found: " + slug));

        List<QuestionConceptTag> tagLinks = questionConceptTagRepository.findByIdConceptTagSlug(slug, Pageable.unpaged()).getContent();
        if (tagLinks.isEmpty()) {
            throw new NoSourceQuestionsException("No questions tagged with '" + slug + "' to ground concept synthesis.");
        }

        List<UUID> questionIds = tagLinks.stream().map(l -> l.getId().getQuestionId()).toList();
        List<Question> questions = questionRepository.findAllById(questionIds);
        List<QuestionAnatomy> anatomies = questionAnatomyRepository.findByQuestionIdIn(questionIds);

        String prompt = buildConceptUnitPrompt(tag, questions, anatomies);
        String effectiveProvider = providerRegistry.resolveStudyProvider(provider);
        AiChatRequest chatRequest = new AiChatRequest(
            List.of(new ChatMessageDto("user", prompt)),
            effectiveProvider,
            model,
            "study"
        );

        UnitGenerationPayload payload = null;
        String parseError = null;
        String lastUsedModel = null;
        int attempts = 0;
        int maxAttempts = 2;

        while (attempts < maxAttempts && payload == null) {
            attempts++;
            try {
                AiChatResponse response = aiChatService.chat(chatRequest, authHeaderKey);
                lastUsedModel = response.model();
                String raw = response.reply();
                String json = anatomyValidationService.extractJson(raw);
                UnitGenerationPayload parsed = objectMapper.readValue(json, UnitGenerationPayload.class);
                anatomyValidationService.validateConceptUnitPayload(parsed);
                anatomyValidationService.validateEmissionCards(parsed.cards());
                payload = parsed;
                parseError = null;
            } catch (Exception e) {
                log.warn("Concept unit generation attempt {} failed for slug '{}': {}", attempts, slug, e.getMessage());
                parseError = e.getMessage() != null ? e.getMessage() : "Failed parsing concept unit JSON";
            }
        }

        if (payload != null) {
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
            unit.setTraps(payload.traps());
            unit.setDeepLinkRef(payload.deepLinkRef());
            unit.setStatus(ConceptUnitStatus.DRAFT);
            unit.setParseError(null);
            unit.setGeneratorModel(lastUsedModel);

            ConceptUnit savedUnit = conceptUnitRepository.save(unit);

            List<RetrievalCard> oldCards = retrievalCardRepository.findByConceptUnitIdOrderByCardOrderAsc(savedUnit.getId());
            if (!oldCards.isEmpty()) {
                retrievalCardRepository.deleteAll(oldCards);
                retrievalCardRepository.flush();
            }

            List<RetrievalCardDto> savedCards = new ArrayList<>();
            List<UnitGenerationCard> cardPayloads = payload.cards() != null ? payload.cards() : Collections.emptyList();
            for (int idx = 0; idx < cardPayloads.size(); idx++) {
                UnitGenerationCard c = cardPayloads.get(idx);
                RetrievalCard card = new RetrievalCard();
                card.setConceptUnit(savedUnit);
                card.setPrompt(c.prompt());
                card.setAnswer(c.answer());
                card.setCardOrder(idx);
                card.setStatus(RetrievalCardStatus.DRAFT);
                RetrievalCard persistedCard = retrievalCardRepository.save(card);
                savedCards.add(toDto(persistedCard));
            }

            return toDto(savedUnit, savedCards);
        } else {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Concept unit generation failed: " + (parseError != null ? parseError : "AI service unavailable")
            );
        }
    }

    private String buildConceptUnitPrompt(
        ConceptTag tag,
        List<Question> questions,
        List<QuestionAnatomy> anatomies
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert computer science professor designing a canonical Concept Unit for GATE CSE preparation.\n");
        sb.append("Concept: ").append(tag.getLabel()).append(" (slug: ").append(tag.getSlug()).append(")\n");
        sb.append("Topic: ").append(tag.getTopic() != null ? tag.getTopic().getName() : "Unknown").append("\n");
        sb.append("\nAggregated Source Questions (").append(questions.size()).append(" questions tagged):\n");
        for (Question q : questions.stream().limit(5).toList()) {
            String stmt = q.getStatement() != null ? q.getStatement() : "";
            sb.append("- [").append(q.getType()).append("] ").append(stmt.length() > 150 ? stmt.substring(0, 150) : stmt).append("...\n");
        }
        if (anatomies != null && !anatomies.isEmpty()) {
            sb.append("\nAggregated Question Traps & Invariants:\n");
            for (QuestionAnatomy a : anatomies.stream().limit(5).toList()) {
                sb.append("- Concept: ").append(a.getTestedConcept()).append("\n");
                if (a.getInvariantRule() != null) {
                    sb.append("  Rule: ").append(a.getInvariantRule()).append("\n");
                }
            }
        }
        sb.append("\nGenerate a complete, rigorous Concept Unit matching this JSON schema:\n")
          .append("{\n")
          .append("  \"title\": \"Human readable title\",\n")
          .append("  \"motivation\": \"Why this concept exists and why it matters in CS/GATE\",\n")
          .append("  \"core_idea\": \"Formal definition and intuitive explanation\",\n")
          .append("  \"invariant\": \"The fundamental mathematical or algorithmic invariant\",\n")
          .append("  \"worked_example\": \"Mandatory detailed worked example (>= 150 words) with numbered steps and concrete values demonstrating solving a canonical problem\",\n")
          .append("  \"traps\": \"Common pitfalls and distractors tested in competitive exams\",\n")
          .append("  \"deep_link_ref\": \"Standard textbook chapter/reference (e.g., Cormen Chapter 15 or Ramakrishnan Chapter 19)\",\n")
          .append("  \"cards\": [\n")
          .append("    {\n")
          .append("      \"prompt\": \"Active recall question forcing the student to retrieve the core mechanism\",\n")
          .append("      \"answer\": \"Concise, precise answer\"\n")
          .append("    },\n")
          .append("    {\n")
          .append("      \"prompt\": \"Second active recall question testing an edge condition or invariant\",\n")
          .append("      \"answer\": \"Precise answer\"\n")
          .append("    }\n")
          .append("  ]\n")
          .append("}\n\n")
          .append("CRITICAL RULES:\n")
          .append("1. worked_example MUST be at least 150 words with numbered steps. Never omit or summarize.\n")
          .append("2. cards array must contain 2 to 4 high-yield active recall flashcards.\n")
          .append("3. No fabricated student percentages.\n")
          .append("4. Output raw JSON only.\n");

        return sb.toString();
    }

    private ConceptUnitDto toDto(ConceptUnit unit, List<RetrievalCardDto> cards) {
        return new ConceptUnitDto(
            unit.getId() != null ? unit.getId() : UUID.randomUUID(),
            unit.getConceptTag() != null ? unit.getConceptTag().getSlug() : "",
            unit.getTitle(),
            unit.getMotivation(),
            unit.getCoreIdea(),
            unit.getInvariant(),
            unit.getWorkedExample(),
            unit.getWorkedExampleExternalId(),
            unit.getTraps(),
            unit.getDeepLinkRef(),
            unit.getStatus(),
            unit.getParseError(),
            unit.getGeneratorModel(),
            unit.getGeneratedAt(),
            cards
        );
    }

    private RetrievalCardDto toDto(RetrievalCard card) {
        return new RetrievalCardDto(
            card.getId() != null ? card.getId() : UUID.randomUUID(),
            card.getPrompt(),
            card.getAnswer(),
            card.getCardOrder(),
            card.getStatus()
        );
    }

    private QuestionSummaryDto toSummary(Question q, QuestionProgress progress) {
        if (q == null) return null;
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
}
