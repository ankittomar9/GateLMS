package com.gatelms.anatomy.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.anatomy.dto.*
import com.gatelms.anatomy.entity.*
import com.gatelms.anatomy.exception.AnatomyLockedException
import com.gatelms.anatomy.exception.AnatomyNotFoundException
import com.gatelms.anatomy.exception.AnatomyValidationException
import com.gatelms.anatomy.repository.*
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionProgressId
import com.gatelms.repository.QuestionProgressRepository
import com.gatelms.repository.QuestionRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AnatomyGenerationService(
    private val questionRepository: QuestionRepository,
    private val questionProgressRepository: QuestionProgressRepository,
    private val questionAnatomyRepository: QuestionAnatomyRepository,
    private val conceptTagRepository: ConceptTagRepository,
    private val questionConceptTagRepository: QuestionConceptTagRepository,
    private val anatomyValidationService: AnatomyValidationService,
    private val aiChatService: AiChatService,
    private val aiRateLimiter: AiRateLimiter,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(AnatomyGenerationService::class.java)

    fun getAnatomy(questionId: UUID, userId: UUID, isAdmin: Boolean): QuestionAnatomyDto {
        // Gating Rule: 200 iff requester has >=1 recorded attempt OR isAdmin
        if (!isAdmin) {
            val progress = questionProgressRepository.findById(QuestionProgressId(userId, questionId))
            val hasAttempted = progress.isPresent && progress.get().lastAttemptedAt != null
            if (!hasAttempted) {
                throw AnatomyLockedException()
            }
        }

        val anatomy = questionAnatomyRepository.findByQuestionId(questionId)
            .orElseThrow { AnatomyNotFoundException("No anatomy dossier found for question $questionId") }

        if (anatomy.status == AnatomyStatus.REJECTED && !isAdmin) {
            throw AnatomyNotFoundException("Anatomy dossier has been rejected")
        }

        return toDto(anatomy)
    }

    @Transactional
    fun generateOrGetCached(
        questionId: UUID,
        userId: UUID,
        provider: String? = null,
        model: String? = null,
        authHeaderKey: String? = null
    ): QuestionAnatomyDto {
        // 1. Return cached if non-REJECTED exists
        val existing = questionAnatomyRepository.findByQuestionId(questionId)
        if (existing.isPresent && existing.get().status != AnatomyStatus.REJECTED) {
            return toDto(existing.get())
        }

        // 2. Check rate limit
        aiRateLimiter.checkAnatomyGenerateLimit(userId)

        // 3. Load question
        val question = questionRepository.findById(questionId)
            .orElseThrow { AnatomyNotFoundException("Question not found with ID: $questionId") }

        // 4. Build prompt
        val systemPrompt = buildSystemPrompt()
        val userPrompt = buildUserPrompt(question)

        val chatRequest = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = userPrompt)),
            provider = provider ?: "GEMINI",
            model = model,
            mode = "study"
        )

        // 5. Generate with 1 internal retry on validation failure
        var rawResponse = ""
        var payload: AnatomyJsonPayload? = null
        var parseError: String? = null
        val resolvedModel = model ?: "gemini-3.8-flash"

        try {
            val response1 = aiChatService.chat(chatRequest, authHeaderKey)
            rawResponse = response1.reply
            payload = anatomyValidationService.parseAndValidate(rawResponse)
        } catch (e1: Exception) {
            log.warn("Anatomy generation first attempt failed for question $questionId: ${e1.message}. Retrying once...")
            try {
                val retryPrompt = "$userPrompt\n\n[CORRECTION REQUIRED]: Your previous output failed validation with: ${e1.message}. Output strictly valid JSON without any markdown formatting or student-population stats."
                val retryRequest = AiChatRequest(
                    messages = listOf(ChatMessageDto(role = "user", content = retryPrompt)),
                    provider = provider ?: "GEMINI",
                    model = model,
                    mode = "study"
                )
                val response2 = aiChatService.chat(retryRequest, authHeaderKey)
                rawResponse = response2.reply
                payload = anatomyValidationService.parseAndValidate(rawResponse)
            } catch (e2: Exception) {
                log.error("Anatomy generation retry also failed for question $questionId: ${e2.message}")
                parseError = e2.message ?: "Failed validation after retry"
            }
        }

        // 6. Persist QuestionAnatomy
        val anatomy = existing.orElseGet {
            QuestionAnatomy(question = question)
        }

        if (payload != null) {
            anatomy.testedConcept = payload.testedConcept
            anatomy.invariantRule = payload.invariantRule
            anatomy.trapAnalysisJson = objectMapper.writeValueAsString(payload.trapAnalysis)
            anatomy.prerequisitesJson = objectMapper.writeValueAsString(payload.prerequisites)
            anatomy.similarPatternHint = payload.similarPatternHint
            anatomy.groundingRefsJson = "[]"
            anatomy.status = AnatomyStatus.DRAFT
            anatomy.parseError = null
            anatomy.generatorModel = resolvedModel
            anatomy.updatedAt = Instant.now()

            val saved = questionAnatomyRepository.save(anatomy)

            // 7. Flywheel emission of concept tags
            emitConceptTags(question, payload.conceptTags)

            return toDto(saved)
        } else {
            // Save as DRAFT with parse_error (never 500 per spec)
            anatomy.testedConcept = "Generation parsing incomplete"
            anatomy.invariantRule = null
            anatomy.trapAnalysisJson = "[]"
            anatomy.prerequisitesJson = "[]"
            anatomy.similarPatternHint = null
            anatomy.groundingRefsJson = "[]"
            anatomy.status = AnatomyStatus.DRAFT
            anatomy.parseError = parseError
            anatomy.generatorModel = resolvedModel
            anatomy.updatedAt = Instant.now()

            val saved = questionAnatomyRepository.save(anatomy)
            return toDto(saved)
        }
    }

    private fun emitConceptTags(question: Question, tags: List<String>) {
        val topic = question.topic
        for (tag in tags) {
            try {
                // Upsert concept_tags
                if (!conceptTagRepository.existsById(tag)) {
                    val label = tag.split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    val newTag = ConceptTag(
                        slug = tag,
                        label = label,
                        topic = topic
                    )
                    conceptTagRepository.save(newTag)
                }

                // Link question_concept_tags
                val qctId = QuestionConceptTagId(question.id!!, tag)
                if (!questionConceptTagRepository.existsById(qctId)) {
                    val link = QuestionConceptTag(
                        id = qctId,
                        source = ConceptTagSource.ANATOMY,
                        confidence = 1.0f
                    )
                    questionConceptTagRepository.save(link)
                }
            } catch (ex: Exception) {
                log.warn("Flywheel concept tag emission ignored error for tag '$tag': ${ex.message}")
            }
        }
    }

    @Transactional
    fun softDeleteAnatomy(questionId: UUID): QuestionAnatomyDto {
        val anatomy = questionAnatomyRepository.findByQuestionId(questionId)
            .orElseThrow { AnatomyNotFoundException("Question anatomy not found for question $questionId") }
        anatomy.status = AnatomyStatus.REJECTED
        anatomy.updatedAt = Instant.now()
        val saved = questionAnatomyRepository.save(anatomy)
        return toDto(saved)
    }

    @Transactional
    fun updateCuration(id: UUID, req: CurationAnatomyUpdateRequest): QuestionAnatomyDto {
        val anatomy = questionAnatomyRepository.findById(id)
            .orElseThrow { AnatomyNotFoundException("Question anatomy not found with id $id") }

        req.status?.let { anatomy.status = it }
        req.testedConcept?.let { anatomy.testedConcept = it }
        req.invariantRule?.let { anatomy.invariantRule = it }
        req.similarPatternHint?.let { anatomy.similarPatternHint = it }
        anatomy.updatedAt = Instant.now()

        val saved = questionAnatomyRepository.save(anatomy)
        return toDto(saved)
    }

    fun getCurationDrafts(pageable: Pageable): Page<QuestionAnatomyDto> {
        return questionAnatomyRepository.findByStatus(AnatomyStatus.DRAFT, pageable).map { toDto(it) }
    }

    fun toDto(anatomy: QuestionAnatomy): QuestionAnatomyDto {
        val trapList: List<TrapAnalysisItem> = try {
            objectMapper.readValue(anatomy.trapAnalysisJson, object : TypeReference<List<TrapAnalysisItem>>() {})
        } catch (e: Exception) {
            emptyList()
        }

        val prereqList: List<PrerequisiteItem> = try {
            objectMapper.readValue(anatomy.prerequisitesJson, object : TypeReference<List<PrerequisiteItem>>() {})
        } catch (e: Exception) {
            emptyList()
        }

        return QuestionAnatomyDto(
            id = anatomy.id ?: UUID.randomUUID(),
            questionId = anatomy.question?.id ?: UUID.randomUUID(),
            testedConcept = anatomy.testedConcept,
            invariantRule = anatomy.invariantRule,
            trapAnalysis = trapList,
            prerequisites = prereqList,
            similarPatternHint = anatomy.similarPatternHint,
            status = anatomy.status,
            parseError = anatomy.parseError,
            generatorModel = anatomy.generatorModel,
            generatedAt = anatomy.generatedAt,
            updatedAt = anatomy.updatedAt
        )
    }

    private fun buildSystemPrompt(): String {
        return """
You are the GateLMS Question Anatomy Engine. Your task is to deconstruct competitive examination questions into an expert anatomical breakdown.

Output ONLY a single valid JSON object strictly complying with this schema:
{
  "tested_concept": "Concise definition (<=300 chars) of the core concept and skill tested",
  "invariant_rule": "The key mathematical or theoretical rule that collapses the problem, or null",
  "trap_analysis": [
    {
      "option": "A",
      "verdict": "INCORRECT",
      "why_plausible": "Why an aspirant might be tempted by this choice",
      "misconception": "The exact conceptual confusion this distractor encodes"
    }
  ],
  "prerequisites": [
    {
      "concept_slug": "kebab-case-slug",
      "concept_label": "Human readable concept name",
      "topic_slug": "topic-slug",
      "must_read": true
    }
  ],
  "concept_tags": ["kebab-case-slug-1", "kebab-case-slug-2"],
  "similar_pattern_hint": "One sentence explaining the question family and how to identify similar questions"
}

CRITICAL RULES:
1. Zero fabricated statistics: Do NOT claim '60% of students choose B' or use percentages/counts about students.
2. Derivability: Distractor analysis must be derived from option text and the key alone. If no trap logic exists, write 'no distractor logic detected'.
3. Invariant: Ground on foundational rules/theorems.
4. No textbook or page number citations.
5. concept_tags: 1 to 4 slugs, all lowercase kebab-case (^[a-z0-9]+(-[a-z0-9]+)*$).
6. If the question is NAT (Numerical Answer Type), trap_analysis should cover common numerical traps (e.g., off-by-one, boundary values, incorrect unit conversion) with option set to 'NAT'.
7. Do not wrap output in markdown fences. Output raw JSON.
        """.trimIndent()
    }

    private fun buildUserPrompt(q: Question): String {
        return buildString {
            appendLine("Deconstruct this question:")
            appendLine("Subject: ${q.subject?.name ?: "Unknown"}")
            appendLine("Topic: ${q.topic?.name ?: "Unknown"}")
            appendLine("Type: ${q.type}")
            appendLine("Marks: ${q.marks}")
            appendLine("Statement:\n${q.statement}")
            if (!q.optionsJson.isNullOrBlank()) {
                appendLine("Options:\n${q.optionsJson}")
            }
            if (!q.correctOptions.isNullOrBlank()) {
                appendLine("Correct Answer: ${q.correctOptions}")
            }
            if (q.numericMin != null || q.numericMax != null) {
                appendLine("Numerical Range: [${q.numericMin} to ${q.numericMax}]")
            }
            if (!q.explanation.isNullOrBlank()) {
                appendLine("Reference Explanation (Untrusted, may contain errors; verify independently):\n${q.explanation}")
            }
        }
    }
}
