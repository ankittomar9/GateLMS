package com.gatelms.anatomy.controller

import com.gatelms.anatomy.dto.*
import com.gatelms.anatomy.service.AnatomyGenerationService
import com.gatelms.anatomy.service.ConceptService
import com.gatelms.dto.QuestionSummaryDto
import com.gatelms.entity.UserRole
import com.gatelms.security.CurrentUserProvider
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class AnatomyGenerateRequest(
    val provider: String? = null,
    val model: String? = null
)

@RestController
@RequestMapping("/api/v1")
class AnatomyController(
    private val anatomyGenerationService: AnatomyGenerationService,
    private val conceptService: ConceptService,
    private val currentUserProvider: CurrentUserProvider
) {

    @GetMapping("/questions/{id}/anatomy")
    fun getAnatomy(@PathVariable id: UUID): ResponseEntity<QuestionAnatomyDto> {
        val user = currentUserProvider.getCurrentUser()
        val isAdmin = user.role == UserRole.ADMIN
        val dto = anatomyGenerationService.getAnatomy(id, user.id, isAdmin)
        return ResponseEntity.ok(dto)
    }

    @PostMapping("/questions/{id}/anatomy/generate")
    fun generateAnatomy(
        @PathVariable id: UUID,
        @RequestBody(required = false) req: AnatomyGenerateRequest?,
        @RequestHeader(value = "x-api-key", required = false) authHeaderKey: String?
    ): ResponseEntity<QuestionAnatomyDto> {
        val user = currentUserProvider.getCurrentUser()
        val dto = anatomyGenerationService.generateOrGetCached(
            questionId = id,
            userId = user.id,
            provider = req?.provider,
            model = req?.model,
            authHeaderKey = authHeaderKey
        )
        return ResponseEntity.ok(dto)
    }

    @DeleteMapping("/questions/{id}/anatomy")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteAnatomy(@PathVariable id: UUID): ResponseEntity<QuestionAnatomyDto> {
        val dto = anatomyGenerationService.softDeleteAnatomy(id)
        return ResponseEntity.ok(dto)
    }

    @GetMapping("/concepts/{slug}")
    fun getConcept(@PathVariable slug: String): ResponseEntity<ConceptUnitDto> {
        val dto = conceptService.getConcept(slug)
        return ResponseEntity.ok(dto)
    }

    @PostMapping("/concepts/{slug}/generate")
    fun generateConcept(
        @PathVariable slug: String,
        @RequestBody(required = false) req: AnatomyGenerateRequest?,
        @RequestHeader(value = "x-api-key", required = false) authHeaderKey: String?
    ): ResponseEntity<ConceptUnitDto> {
        val user = currentUserProvider.getCurrentUser()
        val dto = conceptService.generateOrGetCached(
            slug = slug,
            userId = user.id,
            provider = req?.provider,
            model = req?.model,
            authHeaderKey = authHeaderKey
        )
        return ResponseEntity.ok(dto)
    }

    @GetMapping("/concepts/{slug}/questions")
    fun getConceptQuestions(
        @PathVariable slug: String,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<QuestionSummaryDto>> {
        val page = conceptService.getConceptQuestions(slug, pageable)
        return ResponseEntity.ok(page)
    }

    @GetMapping("/curation/anatomy")
    @PreAuthorize("hasRole('ADMIN')")
    fun getCurationDrafts(
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<QuestionAnatomyDto>> {
        val page = anatomyGenerationService.getCurationDrafts(pageable)
        return ResponseEntity.ok(page)
    }

    @PatchMapping("/curation/anatomy/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateCuration(
        @PathVariable id: UUID,
        @RequestBody req: CurationAnatomyUpdateRequest
    ): ResponseEntity<QuestionAnatomyDto> {
        val dto = anatomyGenerationService.updateCuration(id, req)
        return ResponseEntity.ok(dto)
    }
}
