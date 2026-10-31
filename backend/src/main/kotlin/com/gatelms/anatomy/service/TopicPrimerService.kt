package com.gatelms.anatomy.service

import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.registry.ProviderRegistry
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.anatomy.entity.TopicPrimer
import com.gatelms.anatomy.entity.TopicPrimerStatus
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.TopicPrimerRepository
import com.gatelms.dto.CurationPrimerUpdateRequest
import com.gatelms.dto.TopicPrimerDto
import com.gatelms.dto.TopicPrimerResponseDto
import com.gatelms.entity.Question
import com.gatelms.entity.Topic
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.TopicRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class TopicPrimerService(
    private val topicRepository: TopicRepository,
    private val topicPrimerRepository: TopicPrimerRepository,
    private val conceptTagRepository: ConceptTagRepository,
    private val questionRepository: QuestionRepository,
    private val aiChatService: AiChatService,
    private val aiRateLimiter: AiRateLimiter,
    private val anatomyValidationService: AnatomyValidationService,
    private val aiProperties: com.gatelms.ai.config.AiProperties,
    private val topicGroundingSampler: TopicGroundingSampler = TopicGroundingSampler(),
    private val providerRegistry: ProviderRegistry = ProviderRegistry(aiProperties)
) {
    private val log = LoggerFactory.getLogger(TopicPrimerService::class.java)

    @Transactional(readOnly = true)
    fun getPrimer(topicId: String): TopicPrimerResponseDto {
        val topic = findTopic(topicId)
        val existing = topicPrimerRepository.findByTopicId(topic.id)
        val primerDto = if (existing.isPresent && existing.get().status != TopicPrimerStatus.REJECTED) {
            existing.get().toDto()
        } else {
            null
        }

        val conceptSlugs = conceptTagRepository.findByTopicId(topic.id).map { it.slug }

        return TopicPrimerResponseDto(
            primer = primerDto,
            conceptSlugs = conceptSlugs,
            bookChapterRef = topic.bookChapterRef,
            subjectStandardBookRef = topic.subject?.standardBookRef
        )
    }

    @Transactional
    fun generateOrGetCached(
        topicId: String,
        userId: UUID,
        provider: String? = null,
        model: String? = null,
        authHeaderKey: String? = null,
        force: Boolean = false
    ): TopicPrimerResponseDto {
        val topic = findTopic(topicId)
        val existing = topicPrimerRepository.findByTopicId(topic.id)

        // 1. Cached check: return existing if non-REJECTED, has no parseError, and not forced
        if (!force && existing.isPresent && existing.get().status != TopicPrimerStatus.REJECTED && existing.get().parseError == null) {
            val conceptSlugs = conceptTagRepository.findByTopicId(topic.id).map { it.slug }
            return TopicPrimerResponseDto(
                primer = existing.get().toDto(),
                conceptSlugs = conceptSlugs,
                bookChapterRef = topic.bookChapterRef,
                subjectStandardBookRef = topic.subject?.standardBookRef
            )
        }

        // 2. Rate limit check (429)
        aiRateLimiter.checkPrimerGenerateLimit(userId)

        // 3. Grounding: stratified sample across (type x difficulty) up to 30 questions
        val pool = questionRepository.findSampleQuestionsByTopic(topic.id, PageRequest.of(0, 100))
        val sampleQuestions = topicGroundingSampler.stratifySample(pool, 30)
        val conceptLabels = conceptTagRepository.findByTopicId(topic.id).map { it.label }

        val prompt = buildTopicPrimerPrompt(topic, sampleQuestions, conceptLabels)
        val effectiveProvider = providerRegistry.resolveStudyProvider(provider)
        val chatRequest = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = prompt)),
            provider = effectiveProvider,
            model = model,
            mode = "study"
        )

        // 4. Generate with 1 internal retry
        var payload: TopicPrimerPayload? = null
        var parseError: String? = null
        var lastUsedModel: String? = null
        var attempts = 0
        val maxAttempts = 2

        while (attempts < maxAttempts && payload == null) {
            attempts++
            try {
                val response = aiChatService.chat(chatRequest, authHeaderKey)
                lastUsedModel = response.model
                payload = anatomyValidationService.parseAndValidatePrimer(response.reply)
                parseError = null
            } catch (e: Exception) {
                log.warn("Topic primer generation attempt $attempts failed for topic '${topic.id}': ${e.message}")
                parseError = e.message ?: "Failed primer generation"
            }
        }

        // If generation failed or could not be parsed, DO NOT persist an incomplete/corrupted placeholder to the DB!
        if (payload == null) {
            throw ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Topic primer generation failed: ${parseError ?: "AI service unavailable"}"
            )
        }

        // 5. Persist primer only if successfully generated & validated
        val primer = existing.orElseGet {
            TopicPrimer(topic = topic)
        }

        primer.motivation = payload.motivation
        primer.coreIdea = payload.coreIdea
        primer.invariant = payload.invariant
        primer.workedExample = payload.workedExample
        primer.traps = payload.traps
        primer.status = TopicPrimerStatus.DRAFT
        primer.parseError = null
        primer.generatorModel = lastUsedModel

        val saved = topicPrimerRepository.save(primer)
        val conceptSlugs = conceptTagRepository.findByTopicId(topic.id).map { it.slug }

        return TopicPrimerResponseDto(
            primer = saved.toDto(),
            conceptSlugs = conceptSlugs,
            bookChapterRef = topic.bookChapterRef,
            subjectStandardBookRef = topic.subject?.standardBookRef
        )
    }

    @Transactional
    fun deletePrimer(topicId: String) {
        val topic = findTopic(topicId)
        val existing = topicPrimerRepository.findByTopicId(topic.id)
        if (existing.isPresent) {
            topicPrimerRepository.delete(existing.get())
        }
    }

    @Transactional
    fun updatePrimerByTopic(topicId: String, req: CurationPrimerUpdateRequest): TopicPrimerDto {
        val topic = findTopic(topicId)
        val primer = topicPrimerRepository.findByTopicId(topic.id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Topic primer not found for topic: $topicId") }

        req.status?.let { primer.status = it }
        req.motivation?.let { primer.motivation = it }
        req.coreIdea?.let { primer.coreIdea = it }
        req.invariant?.let { primer.invariant = it }
        req.workedExample?.let { primer.workedExample = it }
        req.traps?.let { primer.traps = it }
        primer.parseError = null

        val saved = topicPrimerRepository.save(primer)
        return saved.toDto()
    }

    @Transactional(readOnly = true)
    fun getCurationDrafts(pageable: Pageable): Page<TopicPrimerDto> {
        return topicPrimerRepository.findByStatus(TopicPrimerStatus.DRAFT, pageable).map { it.toDto() }
    }

    @Transactional
    fun updateCuration(id: UUID, req: CurationPrimerUpdateRequest): TopicPrimerDto {
        val primer = topicPrimerRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Topic primer not found: $id") }

        req.status?.let { primer.status = it }
        req.motivation?.let { primer.motivation = it }
        req.coreIdea?.let { primer.coreIdea = it }
        req.invariant?.let { primer.invariant = it }
        req.workedExample?.let { primer.workedExample = it }
        req.traps?.let { primer.traps = it }

        val saved = topicPrimerRepository.save(primer)
        return saved.toDto()
    }

    private fun findTopic(topicId: String): Topic {
        return topicRepository.findById(topicId)
            .orElseGet {
                topicRepository.findBySlug(topicId)
                    ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found: $topicId")
            }
    }

    internal fun buildTopicPrimerPrompt(
        topic: Topic,
        sampleQuestions: List<Question>,
        conceptLabels: List<String> = emptyList()
    ): String {
        return buildString {
            appendLine("You are an expert computer science professor designing a foundational Topic Orientation and Theory Primer for GATE CSE preparation.")
            appendLine("Topic: ${topic.name} (id: ${topic.id})")
            appendLine("Subject: ${topic.subject?.name ?: "Unknown"}")
            topic.bookChapterRef?.let { appendLine("Standard Reference: $it") }

            if (conceptLabels.isNotEmpty()) {
                appendLine("\nTopic Canonical Concept Taxonomies:")
                conceptLabels.forEach { label ->
                    appendLine("- $label")
                }
            }

            if (sampleQuestions.isNotEmpty()) {
                appendLine("\nGrounding Questions from actual GATE examination pool (${sampleQuestions.size} stratified samples):")
                sampleQuestions.forEachIndexed { idx, q ->
                    appendLine(topicGroundingSampler.formatQuestionSnippet(idx + 1, q, 1500))
                }
            }

            appendLine("""
Generate a complete, rigorous Topic Primer matching this JSON schema:
{
  "motivation": "Why this topic is foundational, what problem it solves in computer science, and its importance in GATE CSE",
  "core_idea": "The central mathematical or algorithmic definitions, properties, and core theorems",
  "invariant": "The fundamental mathematical or structural invariant that always holds",
  "worked_example": "Mandatory detailed canonical problem and complete step-by-step walkthrough (>= 150 words with numbered steps 1., 2., 3. and concrete numerical/symbolic calculations)",
  "traps": "Critical edge cases, common examiner traps, false assumptions, and pitfalls"
}

Important Rules:
1. 'worked_example' must be detailed and >= 150 words with clear numbered steps. A shallow summary will be rejected.
2. Anti-fabrication (AF-1): Never cite student population statistics, exam pass rates, or claim that a percentage/majority of students make a particular mistake (e.g., do NOT write '70% of students fail this' or 'majority of candidates get confused'). Focus purely on the scientific formulas, mathematical derivations, protocols, and technical distractor logic.
3. KaTeX is supported for mathematical notation (\${'$'}math\${'$'}). When writing LaTeX commands in JSON, always escape backslashes (e.g. \\in, \\infty, \\le, \\ge, \\Theta, \\frac).
4. Return ONLY valid JSON.
5. Format with structured Markdown: place code in fenced blocks on their own lines (```c\n...code...\n```), and separate paragraphs and numbered steps with blank lines (\n\n) for clean rendering.
            """.trimIndent())
        }
    }

    private fun TopicPrimer.toDto(): TopicPrimerDto {
        return TopicPrimerDto(
            id = this.id!!,
            topicId = this.topic?.id ?: "",
            motivation = this.motivation,
            coreIdea = this.coreIdea,
            invariant = this.invariant,
            workedExample = this.workedExample,
            traps = this.traps,
            status = this.status.name,
            parseError = this.parseError,
            generatorModel = this.generatorModel,
            generatedAt = this.generatedAt
        )
    }
}
