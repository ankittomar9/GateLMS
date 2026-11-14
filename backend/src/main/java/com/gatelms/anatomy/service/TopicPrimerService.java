package com.gatelms.anatomy.service;

import com.gatelms.ai.config.AiProperties;
import com.gatelms.ai.dto.AiDtos.AiChatRequest;
import com.gatelms.ai.dto.AiDtos.ChatMessageDto;
import com.gatelms.ai.dto.AiDtos.AiChatResponse;
import com.gatelms.ai.registry.ProviderRegistry;
import com.gatelms.ai.security.AiRateLimiter;
import com.gatelms.ai.service.AiChatService;
import com.gatelms.anatomy.dto.AnatomyDtos.TopicPrimerPayload;
import com.gatelms.anatomy.entity.ConceptTag;
import com.gatelms.anatomy.entity.TopicPrimer;
import com.gatelms.anatomy.entity.TopicPrimerStatus;
import com.gatelms.anatomy.repository.ConceptTagRepository;
import com.gatelms.anatomy.repository.TopicPrimerRepository;
import com.gatelms.dto.JourneyDtos.CurationPrimerUpdateRequest;
import com.gatelms.dto.JourneyDtos.TopicPrimerDto;
import com.gatelms.dto.JourneyDtos.TopicPrimerResponseDto;
import com.gatelms.entity.Question;
import com.gatelms.entity.Topic;
import com.gatelms.repository.QuestionRepository;
import com.gatelms.repository.TopicRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class TopicPrimerService {
    private static final Logger log = LoggerFactory.getLogger(TopicPrimerService.class);

    private final TopicRepository topicRepository;
    private final TopicPrimerRepository topicPrimerRepository;
    private final ConceptTagRepository conceptTagRepository;
    private final QuestionRepository questionRepository;
    private final AiChatService aiChatService;
    private final AiRateLimiter aiRateLimiter;
    private final AnatomyValidationService anatomyValidationService;
    private final TopicGroundingSampler topicGroundingSampler;
    private final ProviderRegistry providerRegistry;

    public TopicPrimerService(
        TopicRepository topicRepository,
        TopicPrimerRepository topicPrimerRepository,
        ConceptTagRepository conceptTagRepository,
        QuestionRepository questionRepository,
        AiChatService aiChatService,
        AiRateLimiter aiRateLimiter,
        AnatomyValidationService anatomyValidationService,
        AiProperties aiProperties,
        TopicGroundingSampler topicGroundingSampler,
        ProviderRegistry providerRegistry
    ) {
        this.topicRepository = topicRepository;
        this.topicPrimerRepository = topicPrimerRepository;
        this.conceptTagRepository = conceptTagRepository;
        this.questionRepository = questionRepository;
        this.aiChatService = aiChatService;
        this.aiRateLimiter = aiRateLimiter;
        this.anatomyValidationService = anatomyValidationService;
        this.topicGroundingSampler = topicGroundingSampler != null ? topicGroundingSampler : new TopicGroundingSampler();
        this.providerRegistry = providerRegistry != null ? providerRegistry : new ProviderRegistry(aiProperties);
    }

    @Transactional(readOnly = true)
    public TopicPrimerResponseDto getPrimer(String topicId) {
        Topic topic = findTopic(topicId);
        Optional<TopicPrimer> existing = topicPrimerRepository.findByTopicId(topic.getId());
        TopicPrimerDto primerDto = (existing.isPresent() && existing.get().getStatus() != TopicPrimerStatus.REJECTED)
            ? toDto(existing.get())
            : null;

        List<String> conceptSlugs = conceptTagRepository.findByTopicId(topic.getId()).stream()
            .map(ConceptTag::getSlug).toList();

        return new TopicPrimerResponseDto(
            primerDto,
            conceptSlugs,
            topic.getBookChapterRef(),
            topic.getSubject() != null ? topic.getSubject().getStandardBookRef() : null
        );
    }

    @Transactional
    public TopicPrimerResponseDto generateOrGetCached(
        String topicId,
        UUID userId,
        String provider,
        String model,
        String authHeaderKey,
        boolean force
    ) {
        Topic topic = findTopic(topicId);
        Optional<TopicPrimer> existing = topicPrimerRepository.findByTopicId(topic.getId());

        if (!force && existing.isPresent() && existing.get().getStatus() != TopicPrimerStatus.REJECTED && existing.get().getParseError() == null) {
            List<String> conceptSlugs = conceptTagRepository.findByTopicId(topic.getId()).stream()
                .map(ConceptTag::getSlug).toList();
            return new TopicPrimerResponseDto(
                toDto(existing.get()),
                conceptSlugs,
                topic.getBookChapterRef(),
                topic.getSubject() != null ? topic.getSubject().getStandardBookRef() : null
            );
        }

        aiRateLimiter.checkPrimerGenerateLimit(userId);

        List<Question> pool = questionRepository.findSampleQuestionsByTopic(topic.getId(), PageRequest.of(0, 100));
        List<Question> sampleQuestions = topicGroundingSampler.stratifySample(pool, 30);
        List<String> conceptLabels = conceptTagRepository.findByTopicId(topic.getId()).stream()
            .map(ConceptTag::getLabel).toList();

        String prompt = buildTopicPrimerPrompt(topic, sampleQuestions, conceptLabels);
        String effectiveProvider = providerRegistry.resolveStudyProvider(provider);
        AiChatRequest chatRequest = new AiChatRequest(
            List.of(new ChatMessageDto("user", prompt)),
            effectiveProvider,
            model,
            "study"
        );

        TopicPrimerPayload payload = null;
        String parseError = null;
        String lastUsedModel = null;
        int attempts = 0;
        int maxAttempts = 2;

        while (attempts < maxAttempts && payload == null) {
            attempts++;
            try {
                AiChatResponse response = aiChatService.chat(chatRequest, authHeaderKey);
                lastUsedModel = response.model();
                payload = anatomyValidationService.parseAndValidatePrimer(response.reply());
                parseError = null;
            } catch (Exception e) {
                log.warn("Topic primer generation attempt {} failed for topic '{}': {}", attempts, topicId, e.getMessage());
                parseError = e.getMessage() != null ? e.getMessage() : "Failed parsing topic primer JSON";
            }
        }

        if (payload == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Topic primer generation failed: " + (parseError != null ? parseError : "AI service unavailable")
            );
        }

        TopicPrimer primer = existing.orElseGet(() -> {
            TopicPrimer tp = new TopicPrimer();
            tp.setTopic(topic);
            return tp;
        });

        primer.setMotivation(payload.motivation());
        primer.setCoreIdea(payload.coreIdea());
        primer.setInvariant(payload.invariant());
        primer.setWorkedExample(payload.workedExample());
        primer.setTraps(payload.traps());
        primer.setStatus(TopicPrimerStatus.DRAFT);
        primer.setParseError(null);
        primer.setGeneratorModel(lastUsedModel);

        TopicPrimer saved = topicPrimerRepository.save(primer);
        List<String> conceptSlugs = conceptTagRepository.findByTopicId(topic.getId()).stream()
            .map(ConceptTag::getSlug).toList();

        return new TopicPrimerResponseDto(
            toDto(saved),
            conceptSlugs,
            topic.getBookChapterRef(),
            topic.getSubject() != null ? topic.getSubject().getStandardBookRef() : null
        );
    }

    @Transactional
    public void deletePrimer(String topicId) {
        Topic topic = findTopic(topicId);
        Optional<TopicPrimer> existing = topicPrimerRepository.findByTopicId(topic.getId());
        existing.ifPresent(topicPrimerRepository::delete);
    }

    @Transactional
    public TopicPrimerDto updatePrimerByTopic(String topicId, CurationPrimerUpdateRequest req) {
        Topic topic = findTopic(topicId);
        TopicPrimer primer = topicPrimerRepository.findByTopicId(topic.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic primer not found for topic: " + topicId));

        if (req.status() != null) primer.setStatus(TopicPrimerStatus.valueOf(req.status()));
        if (req.motivation() != null) primer.setMotivation(req.motivation());
        if (req.coreIdea() != null) primer.setCoreIdea(req.coreIdea());
        if (req.invariant() != null) primer.setInvariant(req.invariant());
        if (req.workedExample() != null) primer.setWorkedExample(req.workedExample());
        if (req.traps() != null) primer.setTraps(req.traps());
        primer.setParseError(null);

        TopicPrimer saved = topicPrimerRepository.save(primer);
        return toDto(saved);
    }

    @Transactional
    public TopicPrimerDto updatePrimer(String topicId, CurationPrimerUpdateRequest req) {
        return updatePrimerByTopic(topicId, req);
    }

    @Transactional(readOnly = true)
    public Page<TopicPrimerDto> getCurationDrafts(Pageable pageable) {
        return topicPrimerRepository.findByStatus(TopicPrimerStatus.DRAFT, pageable).map(this::toDto);
    }

    @Transactional
    public TopicPrimerDto updateCuration(UUID id, CurationPrimerUpdateRequest req) {
        TopicPrimer primer = topicPrimerRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic primer not found: " + id));

        if (req.status() != null) primer.setStatus(TopicPrimerStatus.valueOf(req.status()));
        if (req.motivation() != null) primer.setMotivation(req.motivation());
        if (req.coreIdea() != null) primer.setCoreIdea(req.coreIdea());
        if (req.invariant() != null) primer.setInvariant(req.invariant());
        if (req.workedExample() != null) primer.setWorkedExample(req.workedExample());
        if (req.traps() != null) primer.setTraps(req.traps());

        TopicPrimer saved = topicPrimerRepository.save(primer);
        return toDto(saved);
    }

    private Topic findTopic(String topicId) {
        return topicRepository.findById(topicId)
            .orElseGet(() -> {
                Topic bySlug = topicRepository.findBySlug(topicId);
                if (bySlug == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found: " + topicId);
                }
                return bySlug;
            });
    }

    public String buildTopicPrimerPrompt(
        Topic topic,
        List<Question> sampleQuestions,
        List<String> conceptLabels
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an expert computer science professor designing a foundational Topic Orientation and Theory Primer for GATE CSE preparation.\n");
        sb.append("Topic: ").append(topic.getName()).append(" (id: ").append(topic.getId()).append(")\n");
        sb.append("Subject: ").append(topic.getSubject() != null ? topic.getSubject().getName() : "Unknown").append("\n");
        if (topic.getBookChapterRef() != null) {
            sb.append("Standard Reference: ").append(topic.getBookChapterRef()).append("\n");
        }

        if (conceptLabels != null && !conceptLabels.isEmpty()) {
            sb.append("\nTopic Canonical Concept Taxonomies:\n");
            for (String label : conceptLabels) {
                sb.append("- ").append(label).append("\n");
            }
        }

        if (sampleQuestions != null && !sampleQuestions.isEmpty()) {
            sb.append("\nGrounding Questions from actual GATE examination pool (").append(sampleQuestions.size()).append(" stratified samples):\n");
            for (int i = 0; i < sampleQuestions.size(); i++) {
                sb.append(topicGroundingSampler.formatQuestionSnippet(i + 1, sampleQuestions.get(i), 1500)).append("\n");
            }
        }

        sb.append("\nGenerate a complete, rigorous Topic Primer matching this JSON schema:\n")
          .append("{\n")
          .append("  \"motivation\": \"Why this topic is foundational, what problem it solves in computer science, and its importance in GATE CSE\",\n")
          .append("  \"core_idea\": \"The central mathematical or algorithmic definitions, properties, and core theorems\",\n")
          .append("  \"invariant\": \"The fundamental mathematical or structural invariant that always holds\",\n")
          .append("  \"worked_example\": \"Mandatory detailed canonical problem and complete step-by-step walkthrough (>= 150 words with numbered steps 1., 2., 3. and concrete numerical/symbolic calculations)\",\n")
          .append("  \"traps\": \"Critical edge cases, common examiner traps, false assumptions, and pitfalls\"\n")
          .append("}\n\n")
          .append("Important Rules:\n")
          .append("1. 'worked_example' must be detailed and >= 150 words with clear numbered steps. A shallow summary will be rejected.\n")
          .append("2. Anti-fabrication (AF-1): Never cite student population statistics, exam pass rates, or claim that a percentage/majority of students make a particular mistake (e.g., do NOT write '70% of students fail this' or 'majority of candidates get confused'). Focus purely on the scientific formulas, mathematical derivations, protocols, and technical distractor logic.\n")
          .append("3. KaTeX is supported for mathematical notation ($math$). When writing LaTeX commands in JSON, always escape backslashes (e.g. \\in, \\infty, \\le, \\ge, \\Theta, \\frac).\n")
          .append("4. Return ONLY valid JSON.\n")
          .append("5. Format with structured Markdown: place code in fenced blocks on their own lines (```c\\n...code...\\n```), and separate paragraphs and numbered steps with blank lines (\\n\\n) for clean rendering.\n");

        return sb.toString();
    }

    private TopicPrimerDto toDto(TopicPrimer primer) {
        return new TopicPrimerDto(
            primer.getId() != null ? primer.getId() : UUID.randomUUID(),
            primer.getTopic() != null ? primer.getTopic().getId() : "",
            primer.getMotivation(),
            primer.getCoreIdea(),
            primer.getInvariant(),
            primer.getWorkedExample(),
            primer.getTraps(),
            primer.getStatus() != null ? primer.getStatus().name() : "DRAFT",
            primer.getParseError(),
            primer.getGeneratorModel(),
            primer.getGeneratedAt()
        );
    }
}
