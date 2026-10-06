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
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class QuestionService(
    private val questionRepository: QuestionRepository,
    private val subjectRepository: SubjectRepository,
    private val topicRepository: TopicRepository,
    private val objectMapper: ObjectMapper
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
        q: String?,
        pageable: Pageable
    ): Page<QuestionSummaryDto> {
        val normalizedQ = if (q.isNullOrBlank()) null else q.trim()
        val page = questionRepository.findWithFilters(
            subjectId = subjectId?.ifBlank { null },
            topicId = topicId?.ifBlank { null },
            type = type?.ifBlank { null },
            year = year,
            pass1Status = pass1Status?.ifBlank { null },
            pass2Status = pass2Status?.ifBlank { null },
            bookmarked = bookmarked,
            q = normalizedQ,
            pageable = pageable
        )
        return page.map { it.toSummaryDto() }
    }

    @Transactional(readOnly = true)
    fun getQuestionById(id: UUID): QuestionDto {
        val question = questionRepository.findById(id).orElseThrow {
            EntityNotFoundException("Question not found with id: $id")
        }
        return question.toDto()
    }

    fun createQuestion(dto: QuestionUpsertDto): QuestionDto {
        validateQuestion(dto)

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
            gateOverflowUrl = dto.gateOverflowUrl?.trim()
        )

        val saved = questionRepository.save(question)
        return saved.toDto()
    }

    fun updateQuestion(id: UUID, dto: QuestionUpsertDto): QuestionDto {
        validateQuestion(dto)

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
        question.updatedAt = Instant.now()

        val saved = questionRepository.save(question)
        return saved.toDto()
    }

    fun updateStatus(id: UUID, dto: QuestionStatusUpdateDto): QuestionDto {
        val question = questionRepository.findById(id).orElseThrow {
            EntityNotFoundException("Question not found with id: $id")
        }

        dto.pass1Status?.let { question.pass1Status = it }
        dto.pass2Status?.let { question.pass2Status = it }
        dto.isBookmarked?.let { question.isBookmarked = it }
        question.updatedAt = Instant.now()

        val saved = questionRepository.save(question)
        return saved.toDto()
    }

    private fun validateQuestion(dto: QuestionUpsertDto) {
        val errors = mutableMapOf<String, String>()

        if (dto.statement.isBlank()) {
            errors["statement"] = "Statement must not be blank"
        }
        if (dto.marks !in setOf(1, 2)) {
            errors["marks"] = "Marks must be 1 or 2"
        }

        dto.gateOverflowUrl?.let {
            if (it.isNotBlank() && !it.startsWith("http://") && !it.startsWith("https://")) {
                errors["gateOverflowUrl"] = "Must be a valid http or https URL"
            }
        }

        when (dto.type) {
            QuestionType.MCQ -> {
                validateOptions(dto.optionsJson, errors, requiredCount = 4)
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
                validateOptions(dto.optionsJson, errors, requiredCount = 4)
                if (dto.correctOptions.isNullOrBlank()) {
                    errors["correctOptions"] = "MSQ must have at least 1 correct option"
                } else {
                    val opts = dto.correctOptions.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (opts.isEmpty() || opts.size > 4) {
                        errors["correctOptions"] = "MSQ must have between 1 and 4 correct options"
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
            }
        }

        if (errors.isNotEmpty()) {
            throw ValidationException("Question validation failed", errors)
        }
    }

    private fun validateOptions(optionsJson: String?, errors: MutableMap<String, String>, requiredCount: Int) {
        if (optionsJson.isNullOrBlank()) {
            errors["optionsJson"] = "Options must be provided"
            return
        }
        try {
            val list = objectMapper.readValue<List<Map<String, Any>>>(optionsJson)
            if (list.size != requiredCount) {
                errors["optionsJson"] = "Expected exactly $requiredCount options, found ${list.size}"
            }
        } catch (ex: Exception) {
            errors["optionsJson"] = "Invalid options JSON structure: ${ex.message}"
        }
    }

    private fun Question.toSummaryDto(): QuestionSummaryDto {
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
            pass1Status = pass1Status,
            pass2Status = pass2Status,
            isBookmarked = isBookmarked,
            createdAt = createdAt
        )
    }

    private fun Question.toDto(): QuestionDto {
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
            pass1Status = pass1Status,
            pass2Status = pass2Status,
            isBookmarked = isBookmarked,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version
        )
    }
}
