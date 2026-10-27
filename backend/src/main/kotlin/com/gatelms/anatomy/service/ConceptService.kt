package com.gatelms.anatomy.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.anatomy.dto.ConceptUnitDto
import com.gatelms.anatomy.dto.RetrievalCardDto
import com.gatelms.anatomy.entity.*
import com.gatelms.anatomy.exception.AnatomyNotFoundException
import com.gatelms.anatomy.exception.NoSourceQuestionsException
import com.gatelms.anatomy.repository.*
import com.gatelms.dto.QuestionSummaryDto
import com.gatelms.entity.Pass1Status
import com.gatelms.entity.Pass2Status
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionProgress
import com.gatelms.repository.QuestionProgressRepository
import com.gatelms.repository.QuestionRepository
import com.gatelms.security.CurrentUserProvider
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

data class UnitGenerationCard(
    @JsonProperty("prompt") val prompt: String = "",
    @JsonProperty("answer") val answer: String = ""
)

data class UnitGenerationPayload(
    @JsonProperty("title") val title: String = "",
    @JsonProperty("motivation") val motivation: String = "",
    @JsonProperty("core_idea") val coreIdea: String = "",
    @JsonProperty("invariant") val invariant: String? = null,
    @JsonProperty("worked_example") val workedExample: String = "",
    @JsonProperty("traps") val traps: String? = null,
    @JsonProperty("deep_link_ref") val deepLinkRef: String? = null,
    @JsonProperty("cards") val cards: List<UnitGenerationCard> = emptyList()
)

@Service
class ConceptService(
    private val conceptTagRepository: ConceptTagRepository,
    private val questionConceptTagRepository: QuestionConceptTagRepository,
    private val questionAnatomyRepository: QuestionAnatomyRepository,
    private val conceptUnitRepository: ConceptUnitRepository,
    private val retrievalCardRepository: RetrievalCardRepository,
    private val questionRepository: QuestionRepository,
    private val questionProgressRepository: QuestionProgressRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val aiChatService: AiChatService,
    private val aiRateLimiter: AiRateLimiter,
    private val anatomyValidationService: AnatomyValidationService
) {
    private val log = LoggerFactory.getLogger(ConceptService::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    fun getConcept(slug: String): ConceptUnitDto {
        val unit = conceptUnitRepository.findByConceptTagSlug(slug)
            .orElseThrow { AnatomyNotFoundException("Concept unit not found for slug '$slug'") }

        val cards = retrievalCardRepository.findByConceptUnitIdOrderByCardOrderAsc(unit.id!!)
            .map { it.toDto() }

        return unit.toDto(cards)
    }

    @Transactional(readOnly = true)
    fun getConceptQuestions(slug: String, pageable: Pageable): Page<QuestionSummaryDto> {
        val tagLinksPage = questionConceptTagRepository.findByIdConceptTagSlug(slug, pageable)
        val questionIds = tagLinksPage.content.map { it.id.questionId }
        if (questionIds.isEmpty()) {
            return PageImpl(emptyList(), pageable, tagLinksPage.totalElements)
        }

        val questions = questionRepository.findAllById(questionIds).associateBy { it.id }
        val userId = currentUserProvider.getCurrentUserId()
        val progresses = questionProgressRepository.findAllById(
            questionIds.map { com.gatelms.entity.QuestionProgressId(userId, it) }
        ).associateBy { it.id.questionId }

        val summaries = tagLinksPage.content.mapNotNull { link ->
            questions[link.id.questionId]?.toSummary(progresses[link.id.questionId])
        }

        return PageImpl(summaries, pageable, tagLinksPage.totalElements)
    }

    @Transactional
    fun generateOrGetCached(
        slug: String,
        userId: UUID,
        provider: String? = null,
        model: String? = null,
        authHeaderKey: String? = null
    ): ConceptUnitDto {
        // 1. Cached check
        val existing = conceptUnitRepository.findByConceptTagSlug(slug)
        if (existing.isPresent && existing.get().status != ConceptUnitStatus.REJECTED && existing.get().parseError == null) {
            val cards = retrievalCardRepository.findByConceptUnitIdOrderByCardOrderAsc(existing.get().id!!)
                .map { it.toDto() }
            return existing.get().toDto(cards)
        }

        // 2. Rate limit
        aiRateLimiter.checkConceptGenerateLimit(userId)

        // 3. Ensure tag exists and has >= 1 tagged question
        val conceptTag = conceptTagRepository.findById(slug)
            .orElseThrow { AnatomyNotFoundException("Concept tag '$slug' not registered") }

        val taggedLinks = questionConceptTagRepository.findByIdConceptTagSlug(slug, Pageable.unpaged()).content
        if (taggedLinks.isEmpty()) {
            throw NoSourceQuestionsException()
        }

        // 4. Aggregate tagged questions and their anatomy dossiers
        val questionIds = taggedLinks.map { it.id.questionId }
        val questions = questionRepository.findAllById(questionIds)
        val anatomies = questionIds.mapNotNull { questionAnatomyRepository.findByQuestionId(it).orElse(null) }

        val prompt = buildConceptUnitPrompt(conceptTag, questions, anatomies)
        val chatRequest = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = prompt)),
            provider = provider ?: "GEMINI",
            model = model,
            mode = "study"
        )

        // 5. Generate and validate
        var payload: UnitGenerationPayload? = null
        var parseError: String? = null
        val resolvedModel = model ?: "gemini-3.5-flash"

        try {
            val response = aiChatService.chat(chatRequest, authHeaderKey)
            val jsonText = anatomyValidationService.extractJson(response.reply)
            val parsed = objectMapper.readValue(jsonText, UnitGenerationPayload::class.java)

            // PC-1 + card emission contract (SPEC §6.1, §6.4) — enforced by the shared validator
            anatomyValidationService.validateConceptUnitPayload(parsed)

            payload = parsed
        } catch (e: Exception) {
            log.error("Concept unit generation failed for slug '$slug': ${e.message}")
            parseError = e.message ?: "Failed unit generation"
        }

        // 6. Persist ConceptUnit
        val unit = existing.orElseGet {
            ConceptUnit(conceptTag = conceptTag)
        }

        if (payload != null) {
            unit.title = payload.title.ifBlank { conceptTag.label }
            unit.motivation = payload.motivation
            unit.coreIdea = payload.coreIdea
            unit.invariant = payload.invariant
            unit.workedExample = payload.workedExample
            unit.traps = payload.traps
            unit.deepLinkRef = payload.deepLinkRef
            unit.status = ConceptUnitStatus.DRAFT
            unit.parseError = null
            unit.generatorModel = resolvedModel

            val savedUnit = conceptUnitRepository.save(unit)

            // Emit cards
            val savedCards = mutableListOf<RetrievalCardDto>()
            payload.cards.forEachIndexed { idx, c ->
                val card = RetrievalCard(
                    conceptUnit = savedUnit,
                    prompt = c.prompt,
                    answer = c.answer,
                    cardOrder = idx,
                    status = RetrievalCardStatus.DRAFT
                )
                val persistedCard = retrievalCardRepository.save(card)
                savedCards.add(persistedCard.toDto())
            }

            return savedUnit.toDto(savedCards)
        } else {
            throw ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Concept unit generation failed: ${parseError ?: "AI service unavailable"}"
            )
        }
    }

    private fun buildConceptUnitPrompt(
        tag: ConceptTag,
        questions: List<Question>,
        anatomies: List<QuestionAnatomy>
    ): String {
        return buildString {
            appendLine("You are an expert computer science professor designing a canonical Concept Unit for GATE CSE preparation.")
            appendLine("Concept: ${tag.label} (slug: ${tag.slug})")
            appendLine("Topic: ${tag.topic?.name ?: "Unknown"}")
            appendLine("\nAggregated Source Questions (${questions.size} questions tagged):")
            for (q in questions.take(5)) {
                appendLine("- [${q.type}] ${q.statement.take(150)}...")
            }
            if (anatomies.isNotEmpty()) {
                appendLine("\nAggregated Question Traps & Invariants:")
                for (a in anatomies.take(5)) {
                    appendLine("- Concept: ${a.testedConcept}")
                    a.invariantRule?.let { appendLine("  Rule: $it") }
                }
            }
            appendLine("""
Generate a complete, rigorous Concept Unit matching this JSON schema:
{
  "title": "Human readable title",
  "motivation": "Why this concept exists and why it matters in CS/GATE",
  "core_idea": "Formal definition and intuitive explanation",
  "invariant": "The fundamental mathematical or algorithmic invariant",
  "worked_example": "Mandatory detailed worked example (>= 150 words) with numbered steps and concrete values demonstrating solving a canonical problem",
  "traps": "Common pitfalls and distractors tested in competitive exams",
  "deep_link_ref": "Standard textbook chapter/reference (e.g., Cormen Chapter 15 or Ramakrishnan Chapter 19)",
  "cards": [
    {
      "prompt": "Active recall question forcing the student to retrieve the core mechanism",
      "answer": "Concise, precise answer"
    },
    {
      "prompt": "Second active recall question testing an edge condition or invariant",
      "answer": "Precise answer"
    }
  ]
}

CRITICAL RULES:
1. worked_example MUST be at least 150 words with numbered steps. Never omit or summarize.
2. cards array must contain 2 to 4 high-yield active recall flashcards.
3. No fabricated student percentages.
4. Output raw JSON only.
            """.trimIndent())
        }
    }

    private fun ConceptUnit.toDto(cards: List<RetrievalCardDto>): ConceptUnitDto {
        return ConceptUnitDto(
            id = id ?: UUID.randomUUID(),
            conceptTagSlug = conceptTag?.slug ?: "",
            title = title,
            motivation = motivation,
            coreIdea = coreIdea,
            invariant = invariant,
            workedExample = workedExample,
            workedExampleExternalId = workedExampleExternalId,
            traps = traps,
            deepLinkRef = deepLinkRef,
            status = status,
            parseError = parseError,
            generatorModel = generatorModel,
            generatedAt = generatedAt,
            cards = cards
        )
    }

    private fun RetrievalCard.toDto(): RetrievalCardDto {
        return RetrievalCardDto(
            id = id ?: UUID.randomUUID(),
            prompt = prompt,
            answer = answer,
            cardOrder = cardOrder,
            status = status
        )
    }

    private fun Question.toSummary(progress: QuestionProgress?): QuestionSummaryDto {
        return QuestionSummaryDto(
            id = id!!,
            externalId = externalId,
            subjectId = subject?.id,
            subjectName = subject?.name,
            topicId = topic?.id,
            topicName = topic?.name,
            year = year,
            type = type,
            marks = marks,
            statement = statement,
            pass1Status = progress?.pass1Status ?: Pass1Status.PENDING,
            pass2Status = progress?.pass2Status ?: Pass2Status.PENDING,
            isBookmarked = progress?.isBookmarked ?: false,
            bookmarkedAt = progress?.bookmarkedAt,
            lastAttemptedAt = progress?.lastAttemptedAt,
            source = source,
            curationStatus = curationStatus,
            curationNote = curationNote,
            difficulty = difficulty,
            createdAt = createdAt
        )
    }
}
