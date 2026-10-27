package com.gatelms.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.gatelms.dto.*
import com.gatelms.entity.Question
import com.gatelms.entity.QuestionType
import com.gatelms.error.ConflictException
import com.gatelms.error.EntityNotFoundException
import com.gatelms.error.ValidationException
import com.gatelms.repository.QuestionRepository
import com.gatelms.repository.SubjectRepository
import com.gatelms.repository.TopicRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import com.gatelms.entity.Pass1Status
import com.gatelms.entity.Pass2Status
import com.gatelms.entity.QuestionProgress
import com.gatelms.entity.QuestionProgressId
import com.gatelms.repository.QuestionProgressRepository
import com.gatelms.security.CurrentUserProvider
import java.util.UUID

@Service
@Transactional
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val questionProgressRepository: QuestionProgressRepository,
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    private val currentUserProvider: CurrentUserProvider,
    private val objectMapper: ObjectMapper,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val gateLmsMetrics: com.gatelms.metrics.GateLmsMetrics? = null,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val achievementService: AchievementService? = null,
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private val questionConceptTagRepository: com.gatelms.anatomy.repository.QuestionConceptTagRepository? = null
) {

    @Transactional(readOnly = true)
    fun searchQuestions(
        subjectId: String?,
        topicId: String?,
        type: String?,
        year: Int?,
        pass1Status: String?,
        pass2Status: String?,
        bookmarked: Boolean?,
        curationStatus: String? = null,
        source: String? = null,
        difficulty: String? = null,
        q: String?,
        sort: String? = "created_at",
        pageable: Pageable
    ): Page<QuestionSummaryDto> {
        val normalizedQ = if (q.isNullOrBlank()) null else q.trim()
        val sortColumn = when (sort?.trim()?.lowercase()) {
            "year" -> "year"
            "marks" -> "marks"
            "created_at" -> "created_at"
            else -> "created_at"
        }
        val unpagedSortPageable = PageRequest.of(pageable.pageNumber, pageable.pageSize)
        val userId = currentUserProvider.getCurrentUserId()
        val page = questionRepository.findWithFilters(
            userId = userId,
            subjectId = subjectId?.ifBlank { null },
            topicId = topicId?.ifBlank { null },
            type = type?.ifBlank { null },
            year = year,
            pass1Status = pass1Status?.ifBlank { null },
            pass2Status = pass2Status?.ifBlank { null },
            bookmarked = bookmarked,
            curationStatus = curationStatus?.ifBlank { null },
            source = source?.ifBlank { null },
            difficulty = difficulty?.ifBlank { null },
            q = normalizedQ,
            sortColumn = sortColumn,
            pageable = unpagedSortPageable
        )

        val questionIds = page.content.mapNotNull { it.id }
        val progressMap = if (questionIds.isNotEmpty()) {
            questionProgressRepository.findAllByUserIdAndQuestionIdIn(userId, questionIds)
                .associateBy { it.id.questionId }
        } else {
            emptyMap()
        }

        return page.map { it.toSummaryDto(progressMap[it.id]) }
    }

    @Transactional(readOnly = true)
    fun getQuestionById(id: UUID): QuestionDto {
        val question = questionRepository.findById(id).orElseThrow {
            EntityNotFoundException("Question not found with id: $id")
        }
        val userId = currentUserProvider.getCurrentUserId()
        val progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(userId, id).orElse(null)
        val tags = questionConceptTagRepository?.findSlugsByQuestionId(id) ?: emptyList()
        return question.toDto(progress, tags)
    }

    fun createQuestion(dto: QuestionUpsertDto): QuestionDto {
        val validation = validateUpsert(dto)
        if (!validation.valid) {
            throw ValidationException("Question validation failed", validation.fieldErrors)
        }

        val subject = subjectRepository.findById(dto.subjectId).orElseThrow {
            EntityNotFoundException("Subject not found with id: ${dto.subjectId}")
        }
        val topic = topicRepository.findById(dto.topicId).orElseThrow {
            EntityNotFoundException("Topic not found with id: ${dto.topicId}")
        }

        dto.externalId?.let { extId ->
            if (questionRepository.findByExternalId(extId) != null) {
                throw ConflictException("Question with externalId '$extId' already exists")
            }
        }

        val question = Question(
            externalId = dto.externalId?.ifBlank { null },
            subject = subject,
            topic = topic,
            year = dto.year,
            type = dto.type,
            marks = dto.marks,
            statement = dto.statement.trim(),
            optionsJson = dto.optionsJson,
            correctOptions = dto.correctOptions?.trim(),
            numericMin = dto.numericMin,
            numericMax = dto.numericMax,
            explanation = dto.explanation?.trim(),
            gateOverflowUrl = dto.gateOverflowUrl?.trim(),
            source = dto.source ?: "SAMPLE",
            curationStatus = dto.curationStatus ?: "UNREVIEWED",
            curationNote = dto.curationNote?.trim(),
            difficulty = dto.difficulty?.trim()
        )

        val saved = questionRepository.save(question)
        return saved.toDto(null)
    }

    fun updateQuestion(id: UUID, dto: QuestionUpsertDto): QuestionDto {
        val validation = validateUpsert(dto)
        if (!validation.valid) {
            throw ValidationException("Question validation failed", validation.fieldErrors)
        }

        val question = questionRepository.findById(id).orElseThrow {
            EntityNotFoundException("Question not found with id: $id")
        }

        dto.version?.let {
            if (question.version != it) {
                throw ConflictException("Question version mismatch: expected ${question.version}, but got $it")
            }
        }

        val subject = subjectRepository.findById(dto.subjectId).orElseThrow {
            EntityNotFoundException("Subject not found with id: ${dto.subjectId}")
        }
        val topic = topicRepository.findById(dto.topicId).orElseThrow {
            EntityNotFoundException("Topic not found with id: ${dto.topicId}")
        }

        question.externalId = dto.externalId?.ifBlank { null }
        question.subject = subject
        question.topic = topic
        question.year = dto.year
        question.type = dto.type
        question.marks = dto.marks
        question.statement = dto.statement.trim()
        question.optionsJson = dto.optionsJson
        question.correctOptions = dto.correctOptions?.trim()
        question.numericMin = dto.numericMin
        question.numericMax = dto.numericMax
        question.explanation = dto.explanation?.trim()
        question.gateOverflowUrl = dto.gateOverflowUrl?.trim()
        dto.source?.let { question.source = it }
        dto.curationStatus?.let { question.curationStatus = it }
        dto.curationNote?.let { question.curationNote = it.trim() }
        dto.difficulty?.let { question.difficulty = it.trim() }
        question.updatedAt = Instant.now()

        val saved = questionRepository.saveAndFlush(question)
        val userId = currentUserProvider.getCurrentUserId()
        val progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(userId, id).orElse(null)
        return saved.toDto(progress)
    }

    fun updateStatus(id: UUID, dto: QuestionStatusUpdateDto): QuestionDto {
        val question = questionRepository.findById(id).orElseThrow {
            EntityNotFoundException("Question not found with id: $id")
        }

        val userId = currentUserProvider.getCurrentUserId()
        val progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(userId, id).orElseGet {
            QuestionProgress(id = QuestionProgressId(userId = userId, questionId = id))
        }

        dto.pass1Status?.let { progress.pass1Status = it }
        dto.pass2Status?.let { progress.pass2Status = it }
        dto.isBookmarked?.let { progress.isBookmarked = it }
        progress.lastAttemptedAt = Instant.now()

        val savedProgress = questionProgressRepository.saveAndFlush(progress)
        return question.toDto(savedProgress)
    }

    fun updateBookmark(id: UUID, bookmarked: Boolean): QuestionDto {
        val question = questionRepository.findById(id).orElseThrow {
            EntityNotFoundException("Question not found with id: $id")
        }

        val userId = currentUserProvider.getCurrentUserId()
        val progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(userId, id).orElseGet {
            QuestionProgress(id = QuestionProgressId(userId = userId, questionId = id))
        }

        progress.isBookmarked = bookmarked
        progress.bookmarkedAt = if (bookmarked) Instant.now() else null

        val savedProgress = questionProgressRepository.saveAndFlush(progress)
        return question.toDto(savedProgress)
    }

    fun updateCuration(id: UUID, dto: CurationUpdateRequest): QuestionDto {
        val currentUser = currentUserProvider.getCurrentUser()
        if (currentUser.role != com.gatelms.entity.UserRole.ADMIN) {
            throw com.gatelms.error.ForbiddenException("Only ADMIN users can curate questions")
        }

        val question = questionRepository.findById(id).orElseThrow {
            EntityNotFoundException("Question not found with id: $id")
        }

        val validStatuses = setOf("ACCEPTED", "REJECTED", "UNREVIEWED")
        val status = dto.status?.trim()?.uppercase()
        if (status == null || status !in validStatuses) {
            throw ValidationException(
                "Invalid curation status",
                mapOf("curationStatus" to "Must be one of ACCEPTED, REJECTED, UNREVIEWED")
            )
        }

        question.curationStatus = status
        question.curationNote = dto.note?.trim()
        question.updatedAt = Instant.now()

        if (status == "ACCEPTED" || status == "REJECTED") {
            gateLmsMetrics?.recordCurationDecision(status.lowercase())
            try {
                achievementService?.checkOnCurationDecision()
            } catch (e: Exception) {
                // Ignore failure in achievement check
            }
        }

        val saved = questionRepository.saveAndFlush(question)
        val progress = questionProgressRepository.findByIdUserIdAndIdQuestionId(currentUser.id!!, id).orElse(null)
        return saved.toDto(progress)
    }

    @Transactional(readOnly = true)
    fun validateUpsert(dto: QuestionUpsertDto): QuestionValidationResultDto {
        val errors = mutableMapOf<String, String>()
        val warnings = mutableListOf<String>()

        if (dto.statement.isBlank()) {
            errors["statement"] = "Statement must not be blank"
        }
        if (dto.marks !in setOf(1, 2)) {
            errors["marks"] = "Marks must be 1 or 2"
        }
        if (dto.subjectId.isBlank()) {
            errors["subjectId"] = "subjectId is required"
        }
        if (dto.topicId.isBlank()) {
            errors["topicId"] = "topicId is required"
        }

        dto.gateOverflowUrl?.let {
            if (it.isNotBlank() && !it.startsWith("http://") && !it.startsWith("https://")) {
                errors["gateOverflowUrl"] = "Must be a valid http or https URL"
            }
        }

        when (dto.type) {
            QuestionType.MCQ -> {
                validateOptions(dto.optionsJson, errors, warnings, isMcq = true)
                if (dto.correctOptions.isNullOrBlank()) {
                    errors["correctOptions"] = "MCQ must have exactly 1 correct option"
                } else {
                    val opts = dto.correctOptions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (opts.size != 1) {
                        errors["correctOptions"] = "MCQ must have exactly 1 correct option"
                    }
                }
                if (dto.numericMin != null || dto.numericMax != null) {
                    errors["numericMin"] = "MCQ questions must not have numericMin/numericMax"
                }
            }
            QuestionType.MSQ -> {
                validateOptions(dto.optionsJson, errors, warnings, isMcq = false)
                if (dto.correctOptions.isNullOrBlank()) {
                    errors["correctOptions"] = "MSQ must have at least 1 correct option"
                } else {
                    val opts = dto.correctOptions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (opts.isEmpty() || opts.size > 6) {
                        errors["correctOptions"] = "MSQ must have between 1 and 6 correct options"
                    }
                    if (opts.distinct().size != opts.size) {
                        errors["correctOptions"] = "Duplicate options specified in correctOptions"
                    }
                }
                if (dto.numericMin != null || dto.numericMax != null) {
                    errors["numericMin"] = "MSQ questions must not have numericMin/numericMax"
                }
            }
            QuestionType.NAT -> {
                if (dto.numericMin == null || dto.numericMax == null) {
                    errors["numericMin"] = "NAT questions require both numericMin and numericMax"
                } else if (dto.numericMin > dto.numericMax) {
                    errors["numericMin"] = "numericMin must be less than or equal to numericMax"
                }
                if (!dto.optionsJson.isNullOrBlank()) {
                    errors["optionsJson"] = "NAT questions must not have options"
                }
                if (!dto.correctOptions.isNullOrBlank()) {
                    errors["correctOptions"] = "NAT questions must not have correctOptions"
                }
                if (dto.numericMin != null && dto.numericMax != null && (dto.numericMax - dto.numericMin) > 1000.0) {
                    warnings.add("NAT tolerance span (${dto.numericMax - dto.numericMin}) > 1000 flagged as suspicious")
                }
            }
        }

        // Warnings (non-blocking)
        if (dto.statement.contains("$$")) {
            warnings.add("Display-math ($$...$$) detected in statement")
        } else {
            warnings.add("Display-math ($$...$$) absent in statement")
        }

        if (dto.explanation.isNullOrBlank()) {
            warnings.add("Explanation is empty")
        }

        dto.externalId?.let { extId ->
            if (extId.isNotBlank()) {
                try {
                    val existing = questionRepository.findByExternalId(extId)
                    if (existing != null) {
                        warnings.add("External ID '$extId' already exists in question bank")
                    }
                } catch (_: Exception) {
                    // Ignored if repository query cannot be answered (e.g. unstubbed mock in unit test)
                }
            }
        }

        return QuestionValidationResultDto(
            valid = errors.isEmpty(),
            fieldErrors = errors,
            warnings = warnings
        )
    }

    private fun validateOptions(
        optionsJson: String?, 
        errors: MutableMap<String, String>, 
        warnings: MutableList<String>,
        isMcq: Boolean
    ) {
        if (optionsJson.isNullOrBlank()) {
            errors["optionsJson"] = "Options must be provided"
            return
        }
        try {
            val list = objectMapper.readValue<List<Map<String, Any>>>(optionsJson)
            if (list.size < 2) {
                errors["optionsJson"] = "At least 2 options are required, found ${list.size}"
            } else if (list.size > 6) {
                errors["optionsJson"] = "At most 6 options are allowed, found ${list.size}"
            } else if (isMcq && list.size < 4) {
                warnings.add("Options count (${list.size}) < 4 for MCQ")
            }
        } catch (ex: Exception) {
            errors["optionsJson"] = "Invalid options JSON structure: ${ex.message}"
        }
    }

    fun Question.toSummaryDto(progress: QuestionProgress? = null): QuestionSummaryDto {
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

    private fun Question.toDto(progress: QuestionProgress? = null, conceptTags: List<String>? = null): QuestionDto {
        val resolvedTags = conceptTags ?: (questionConceptTagRepository?.findSlugsByQuestionId(id!!) ?: emptyList())
        return QuestionDto(
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
            optionsJson = optionsJson,
            correctOptions = correctOptions,
            numericMin = numericMin,
            numericMax = numericMax,
            explanation = explanation,
            gateOverflowUrl = gateOverflowUrl,
            pass1Status = progress?.pass1Status ?: Pass1Status.PENDING,
            pass2Status = progress?.pass2Status ?: Pass2Status.PENDING,
            isBookmarked = progress?.isBookmarked ?: false,
            bookmarkedAt = progress?.bookmarkedAt,
            lastAttemptedAt = progress?.lastAttemptedAt,
            source = source,
            curationStatus = curationStatus,
            curationNote = curationNote,
            difficulty = difficulty,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
            conceptTags = resolvedTags
        )
    }
}
