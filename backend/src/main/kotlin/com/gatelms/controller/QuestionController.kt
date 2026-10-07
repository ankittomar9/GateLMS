package com.gatelms.controller

import com.gatelms.dto.QuestionDto
import com.gatelms.dto.QuestionStatusUpdateDto
import com.gatelms.dto.QuestionSummaryDto
import com.gatelms.dto.QuestionUpsertDto
import com.gatelms.service.QuestionService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/questions")
class QuestionController(
    private val questionService: QuestionService
) {

    @GetMapping
    fun searchQuestions(
        @RequestParam(required = false) subjectId: String?,
        @RequestParam(required = false) topicId: String?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) pass1Status: String?,
        @RequestParam(required = false) pass2Status: String?,
        @RequestParam(required = false) bookmarked: Boolean?,
        @RequestParam(required = false) curationStatus: String?,
        @RequestParam(required = false) source: String?,
        @RequestParam(required = false) difficulty: String?,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false, defaultValue = "created_at") sort: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): Page<QuestionSummaryDto> {
        val allowedSorts = setOf("created_at", "year", "marks")
        val normalizedSort = if (sort.isNullOrBlank()) "created_at" else sort.trim().lowercase()
        if (normalizedSort !in allowedSorts) {
            throw com.gatelms.error.ValidationException(
                "Invalid sort parameter",
                mapOf("sort" to "Must be one of: created_at, year, marks")
            )
        }
        return questionService.searchQuestions(
            subjectId = subjectId,
            topicId = topicId,
            type = type,
            year = year,
            pass1Status = pass1Status,
            pass2Status = pass2Status,
            bookmarked = bookmarked,
            curationStatus = curationStatus,
            source = source,
            difficulty = difficulty,
            q = q,
            sort = normalizedSort,
            pageable = pageable
        )
    }

    @GetMapping("/{id}")
    fun getQuestionById(@PathVariable id: UUID): QuestionDto {
        return questionService.getQuestionById(id)
    }

    @PostMapping("/validate")
    fun validateQuestion(@RequestBody dto: QuestionUpsertDto): com.gatelms.dto.QuestionValidationResultDto {
        return questionService.validateUpsert(dto)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createQuestion(@Valid @RequestBody dto: QuestionUpsertDto): QuestionDto {
        return questionService.createQuestion(dto)
    }

    @PutMapping("/{id}")
    fun updateQuestion(
        @PathVariable id: UUID,
        @Valid @RequestBody dto: QuestionUpsertDto
    ): QuestionDto {
        return questionService.updateQuestion(id, dto)
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestBody dto: QuestionStatusUpdateDto
    ): QuestionDto {
        return questionService.updateStatus(id, dto)
    }

    @PatchMapping("/{id}/curation")
    fun updateCuration(
        @PathVariable id: UUID,
        @RequestBody dto: com.gatelms.dto.CurationUpdateRequest
    ): QuestionDto {
        return questionService.updateCuration(id, dto)
    }
}
