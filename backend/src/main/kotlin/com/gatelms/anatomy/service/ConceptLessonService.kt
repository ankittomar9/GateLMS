package com.gatelms.anatomy.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.gatelms.ai.dto.AiChatRequest
import com.gatelms.ai.dto.ChatMessageDto
import com.gatelms.ai.security.AiRateLimiter
import com.gatelms.ai.service.AiChatService
import com.gatelms.anatomy.dto.*
import com.gatelms.anatomy.entity.ConceptTag
import com.gatelms.anatomy.entity.ConceptUnit
import com.gatelms.anatomy.entity.ConceptUnitStatus
import com.gatelms.anatomy.exception.AnatomyValidationException
import com.gatelms.anatomy.repository.ConceptTagRepository
import com.gatelms.anatomy.repository.ConceptUnitRepository
import com.gatelms.anatomy.repository.QuestionConceptTagRepository
import com.gatelms.anatomy.repository.TopicPrimerRepository
import com.gatelms.entity.Question
import com.gatelms.repository.QuestionRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class ConceptLessonService(
    private val conceptTagRepository: ConceptTagRepository,
    private val conceptUnitRepository: ConceptUnitRepository,
    private val questionConceptTagRepository: QuestionConceptTagRepository,
    private val questionRepository: QuestionRepository,
    private val topicPrimerRepository: TopicPrimerRepository,
    private val aiChatService: AiChatService,
    private val aiRateLimiter: AiRateLimiter,
    private val aiProperties: com.gatelms.ai.config.AiProperties,
    private val topicGroundingSampler: TopicGroundingSampler = TopicGroundingSampler()
) {
    private val log = LoggerFactory.getLogger(ConceptLessonService::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val populationStatRegex = Regex(
        "(?i)(\\d+\\s*(%|percent)\\s+(of\\s+)?(students|candidates|aspirants|test-takers|examinees|cohort|learners)|" +
        "\\b(majority|minority|most|many)\\s+of\\s+(students|candidates|aspirants|test-takers|examinees)\\b|" +
        "\\b(students|candidates|aspirants|test-takers|examinees)\\s+[^.\\n]{0,60}\\b\\d+\\s*(%|percent))"
    )

    @Transactional(readOnly = true)
    fun getLesson(slug: String): ConceptLessonResponseDto {
        val tag = findConceptTag(slug)
        val existing = conceptUnitRepository.findByConceptTagSlug(tag.slug)
        val lessonDto = if (existing.isPresent && existing.get().status != ConceptUnitStatus.REJECTED) {
            existing.get().toLessonDto(tag)
        } else {
            null
        }

        val mappedQuestionItems = getMappedQuestionItems(tag.slug)

        return ConceptLessonResponseDto(
            lesson = lessonDto,
            conceptTagSlug = tag.slug,
            conceptTagLabel = tag.label,
            topicId = tag.topic?.id ?: "",
            topicName = tag.topic?.name ?: "",
            mappedQuestionCount = mappedQuestionItems.size,
            mappedQuestions = mappedQuestionItems
        )
    }

    @Transactional
    fun generateOrGetCached(
        slug: String,
        userId: UUID,
        provider: String? = null,
        model: String? = null,
        authHeaderKey: String? = null,
        force: Boolean = false
    ): ConceptLessonResponseDto {
        val tag = findConceptTag(slug)
        val existing = conceptUnitRepository.findByConceptTagSlug(tag.slug)

        // 1. Cached check: return existing if non-REJECTED, has no parseError, and not forced
        if (!force && existing.isPresent && existing.get().status != ConceptUnitStatus.REJECTED && existing.get().parseError == null) {
            val mappedQuestionItems = getMappedQuestionItems(tag.slug)
            return ConceptLessonResponseDto(
                lesson = existing.get().toLessonDto(tag),
                conceptTagSlug = tag.slug,
                conceptTagLabel = tag.label,
                topicId = tag.topic?.id ?: "",
                topicName = tag.topic?.name ?: "",
                mappedQuestionCount = mappedQuestionItems.size,
                mappedQuestions = mappedQuestionItems
            )
        }

        // 2. Rate limit check (REUSE existing primer limit bucket per SPEC §5.4)
        aiRateLimiter.checkPrimerGenerateLimit(userId)

        // 3. Grounding: fetch mapped questions & topic primer context
        val tagLinks = questionConceptTagRepository.findByIdConceptTagSlug(tag.slug, Pageable.unpaged()).content
        if (tagLinks.isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot generate concept lesson: No questions in the bank have been mapped to '$slug' yet."
            )
        }

        val questionIds = tagLinks.map { it.id.questionId }
        val allMappedQuestions = questionRepository.findAllById(questionIds)
            .filter { it.curationStatus != "REJECTED" }

        if (allMappedQuestions.isEmpty()) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Cannot generate concept lesson: All mapped questions for '$slug' are rejected."
            )
        }

        // Stratified sample up to 15 questions across (type x difficulty)
        val sampleQuestions = topicGroundingSampler.stratifySample(allMappedQuestions, 15)
        val validExternalIds = allMappedQuestions.mapNotNull { it.externalId }.toSet()

        val topicPrimerCoreIdea = tag.topic?.id?.let {
            topicPrimerRepository.findByTopicId(it).orElse(null)?.coreIdea
        }

        val prompt = buildConceptLessonPrompt(tag, sampleQuestions, topicPrimerCoreIdea)
        val chatRequest = AiChatRequest(
            messages = listOf(ChatMessageDto(role = "user", content = prompt)),
            provider = provider ?: "GEMINI",
            model = model,
            mode = "study"
        )

        // 4. Generate with 1 internal retry (max 2 attempts)
        val resolvedModel = model ?: aiProperties.providers.gemini.defaultModel.ifBlank { "gemini-3.5-flash" }
        var payload: ConceptLessonPayload? = null
        var parseError: String? = null
        var attempts = 0
        val maxAttempts = 2

        while (attempts < maxAttempts && payload == null) {
            attempts++
            try {
                val response = aiChatService.chat(chatRequest, authHeaderKey)
                payload = parseAndValidateLessonPayload(response.reply, validExternalIds)
                parseError = null
            } catch (e: Exception) {
                log.warn("Concept lesson generation attempt $attempts failed for '$slug': ${e.message}")
                parseError = e.message ?: "Failed concept lesson generation"
            }
        }

        // Never persist failed/corrupted placeholders to DB
        if (payload == null) {
            throw ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Concept lesson generation failed: ${parseError ?: "AI service unavailable"}"
            )
        }

        // 5. Persist lesson only when successfully validated
        val unit = existing.orElseGet {
            ConceptUnit(conceptTag = tag)
        }

        unit.title = payload.title.ifBlank { tag.label }
        unit.motivation = payload.motivation
        unit.coreIdea = payload.coreIdea
        unit.invariant = payload.invariant
        unit.workedExample = payload.workedExample
        unit.workedExampleExternalId = payload.workedExampleExternalId
        unit.traps = payload.traps
        unit.status = ConceptUnitStatus.DRAFT
        unit.parseError = null
        unit.generatorModel = resolvedModel

        val saved = conceptUnitRepository.save(unit)
        val mappedQuestionItems = getMappedQuestionItems(tag.slug)

        return ConceptLessonResponseDto(
            lesson = saved.toLessonDto(tag),
            conceptTagSlug = tag.slug,
            conceptTagLabel = tag.label,
            topicId = tag.topic?.id ?: "",
            topicName = tag.topic?.name ?: "",
            mappedQuestionCount = mappedQuestionItems.size,
            mappedQuestions = mappedQuestionItems
        )
    }

    @Transactional
    fun updateLesson(slug: String, req: CurationLessonUpdateRequest): ConceptLessonDto {
        val tag = findConceptTag(slug)
        val unit = conceptUnitRepository.findByConceptTagSlug(tag.slug)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Concept lesson not found for slug: $slug") }

        req.status?.let { unit.status = it }
        req.title?.let { unit.title = it }
        req.motivation?.let { unit.motivation = it }
        req.coreIdea?.let { unit.coreIdea = it }
        req.invariant?.let { unit.invariant = it }
        req.workedExample?.let { unit.workedExample = it }
        req.workedExampleExternalId?.let { unit.workedExampleExternalId = it }
        req.traps?.let { unit.traps = it }
        unit.parseError = null

        val saved = conceptUnitRepository.save(unit)
        return saved.toLessonDto(tag)
    }

    @Transactional
    fun deleteLesson(slug: String) {
        val tag = findConceptTag(slug)
        val existing = conceptUnitRepository.findByConceptTagSlug(tag.slug)
        if (existing.isPresent) {
            conceptUnitRepository.delete(existing.get())
        }
    }

    @Transactional(readOnly = true)
    fun getCurationLessons(pageable: Pageable): Page<ConceptLessonDto> {
        val tagsMap = conceptTagRepository.findAll().associateBy { it.slug }
        return conceptUnitRepository.findAll(pageable).map { cu ->
            val tag = cu.conceptTag?.slug?.let { tagsMap[it] } ?: ConceptTag(slug = cu.conceptTag?.slug ?: "", label = cu.title)
            cu.toLessonDto(tag)
        }
    }

    internal fun buildConceptLessonPrompt(
        tag: ConceptTag,
        sampleQuestions: List<Question>,
        topicPrimerCoreIdea: String?
    ): String {
        return buildString {
            appendLine("You are an expert computer science professor designing a rigorous, canonical Concept Lesson for GATE CSE preparation.")
            appendLine("Target Concept: ${tag.label} (slug: ${tag.slug})")
            appendLine("Topic: ${tag.topic?.name ?: "Unknown"}")
            appendLine("Subject: ${tag.topic?.subject?.name ?: "Unknown"}")

            if (!topicPrimerCoreIdea.isNullOrBlank()) {
                appendLine("\nTopic Orientation Context (Background Core Idea):")
                appendLine(topicPrimerCoreIdea.trim())
            }

            if (sampleQuestions.isNotEmpty()) {
                appendLine("\nAvailable Mapped Questions for this Concept (${sampleQuestions.size} stratified samples):")
                sampleQuestions.forEachIndexed { idx, q ->
                    appendLine(topicGroundingSampler.formatQuestionSnippet(idx + 1, q, 1500))
                }
            }

            appendLine("""
Generate a complete Concept Lesson matching this JSON schema:
{
  "title": "${tag.label}",
  "motivation": "Plain-language intuition explaining why this concept exists, the concurrency or systems problem it solves, and its role in GATE CSE",
  "core_idea": "The central mathematical, invariant, or algorithmic formalisms and operational definitions with KaTeX formulas ($...$)",
  "invariant": "The fundamental logical invariant or rule that must hold across all execution interleavings",
  "worked_example": "Mandatory step-by-step walkthrough of ONE canonical problem adapted from the mapped question list above (detailed numbered steps 1., 2., 3. with concrete states/numbers)",
  "worked_example_external_id": "The EXACT externalId of the mapped question adapted for the worked example (must be one of the IDs provided in brackets above, e.g. 'SO-OS-0001')",
  "traps": "2 to 4 critical examiner traps, subtle race/deadlock edge cases, or common false assumptions"
}

Important Rules (Binding PC-2 and AF-1 Contract):
1. 'worked_example_external_id' MUST be chosen from the provided mapped question list and match its externalId character-for-character.
2. The entire lesson content ('motivation' + 'core_idea' + 'invariant' + 'worked_example' + 'traps') must be between 300 and 600 words total.
3. 'worked_example' must be a detailed, concrete calculation/derivation walkthrough with numbered steps.
4. Anti-fabrication (AF-1): Do NOT invent student statistics, exam pass rates, or population percentages (e.g., do NOT write '70% of students fail this').
5. KaTeX formulas ($...$) are supported for mathematical and logic notation.
6. Return ONLY valid JSON.
            """.trimIndent())
        }
    }

    internal fun parseAndValidateLessonPayload(
        rawResponse: String,
        validExternalIds: Set<String>
    ): ConceptLessonPayload {
        val jsonText = extractJson(rawResponse)
        val payload = try {
            objectMapper.readValue(jsonText, ConceptLessonPayload::class.java)
        } catch (e: Exception) {
            throw AnatomyValidationException("Malformed or unparseable JSON: ${e.message}")
        }

        validateLessonPayload(payload, validExternalIds)
        return payload
    }

    internal fun validateLessonPayload(
        payload: ConceptLessonPayload,
        validExternalIds: Set<String>
    ) {
        val errors = mutableListOf<String>()

        if (payload.motivation.isBlank()) {
            errors.add("motivation must not be blank")
        }
        if (payload.coreIdea.isBlank()) {
            errors.add("core_idea must not be blank")
        }
        if (payload.workedExample.isBlank()) {
            errors.add("worked_example must not be blank")
        }

        // 1. Worked example external ID validation (PC-2 hard validation)
        val extId = payload.workedExampleExternalId.trim()
        if (extId.isBlank()) {
            errors.add("PC-2 violation: worked_example_external_id must not be blank and must cite a mapped question")
        } else if (validExternalIds.isNotEmpty() && !validExternalIds.contains(extId)) {
            errors.add("PC-2 violation: cited worked_example_external_id '$extId' is not mapped to this concept")
        }

        // 2. Word count validation: total 300-600 words (PC-2)
        val totalWords = countWords(
            payload.motivation,
            payload.coreIdea,
            payload.invariant ?: "",
            payload.workedExample,
            payload.traps ?: ""
        )
        if (totalWords < 300 || totalWords > 600) {
            errors.add("PC-2 violation: total lesson word count must be between 300 and 600 words (actual: $totalWords words)")
        }

        // 3. AF-1 Anti-fabrication of student population statistics
        val allText = buildString {
            append(payload.motivation).append(" ")
            append(payload.coreIdea).append(" ")
            payload.invariant?.let { append(it).append(" ") }
            append(payload.workedExample).append(" ")
            payload.traps?.let { append(it).append(" ") }
        }
        if (populationStatRegex.containsMatchIn(allText)) {
            errors.add("AF-1 violation: lesson contains fabricated student-population statistics or percentage claims")
        }

        if (errors.isNotEmpty()) {
            throw AnatomyValidationException(errors.joinToString("; "))
        }
    }

    private fun countWords(vararg texts: String): Int {
        return texts.sumOf { text ->
            text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        }
    }

    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed
        }
        val codeFenceMatch = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```").find(trimmed)
        if (codeFenceMatch != null) {
            return codeFenceMatch.groupValues[1].trim()
        }
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return trimmed
    }

    private fun findConceptTag(slug: String): ConceptTag {
        return conceptTagRepository.findById(slug)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Concept tag not found: $slug") }
    }

    private fun getMappedQuestionItems(slug: String): List<ConceptMappedQuestionItemDto> {
        val links = questionConceptTagRepository.findByIdConceptTagSlug(slug, Pageable.unpaged()).content
        if (links.isEmpty()) return emptyList()
        val questionIds = links.map { it.id.questionId }
        val questions = questionRepository.findAllById(questionIds)
            .filter { it.curationStatus != "REJECTED" }
        return questions.map { q ->
            ConceptMappedQuestionItemDto(
                id = q.id!!,
                externalId = q.externalId ?: "",
                statement = q.statement,
                type = q.type.name,
                difficulty = q.difficulty,
                year = q.year
            )
        }
    }

    private fun ConceptUnit.toLessonDto(tag: ConceptTag): ConceptLessonDto {
        return ConceptLessonDto(
            id = this.id!!,
            conceptTagSlug = tag.slug,
            conceptTagLabel = tag.label,
            title = this.title.ifBlank { tag.label },
            motivation = this.motivation,
            coreIdea = this.coreIdea,
            invariant = this.invariant,
            workedExample = this.workedExample,
            workedExampleExternalId = this.workedExampleExternalId,
            traps = this.traps,
            status = this.status.name,
            parseError = this.parseError,
            generatorModel = this.generatorModel,
            generatedAt = this.generatedAt
        )
    }
}
