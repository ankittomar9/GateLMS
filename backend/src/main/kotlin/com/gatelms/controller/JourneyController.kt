package com.gatelms.controller

import com.gatelms.anatomy.dto.*
import com.gatelms.anatomy.service.ConceptCoverageService
import com.gatelms.anatomy.service.TopicPrimerService
import com.gatelms.dto.*
import com.gatelms.security.CurrentUserProvider
import com.gatelms.service.JourneyService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class JourneyController(
    private val journeyService: JourneyService,
    private val topicPrimerService: TopicPrimerService,
    private val currentUserProvider: CurrentUserProvider,
    private val conceptCoverageService: ConceptCoverageService,
    private val conceptLessonService: com.gatelms.anatomy.service.ConceptLessonService
) {
    @GetMapping("/journey/overview")
    fun getOverview(): ResponseEntity<List<JourneyOverviewSubjectDto>> {
        return ResponseEntity.ok(journeyService.getOverview())
    }

    @GetMapping("/journey/subjects/{subjectId}/topics")
    fun getSubjectTopics(@PathVariable subjectId: String): ResponseEntity<List<JourneyTopicSummaryDto>> {
        return ResponseEntity.ok(journeyService.getSubjectTopics(subjectId))
    }

    @GetMapping("/journey/topics/{topicId}/primer")
    fun getPrimer(@PathVariable topicId: String): ResponseEntity<TopicPrimerResponseDto> {
        return ResponseEntity.ok(topicPrimerService.getPrimer(topicId))
    }

    @PostMapping("/journey/topics/{topicId}/primer/generate")
    fun generatePrimer(
        @PathVariable topicId: String,
        @RequestParam(required = false) provider: String?,
        @RequestParam(required = false) model: String?,
        @RequestParam(defaultValue = "false") force: Boolean,
        @RequestHeader(value = "X-Api-Key", required = false) authHeaderKey: String?
    ): ResponseEntity<TopicPrimerResponseDto> {
        val userId = currentUserProvider.getCurrentUserId()
        val res = topicPrimerService.generateOrGetCached(topicId, userId, provider, model, authHeaderKey, force)
        return ResponseEntity.ok(res)
    }

    @PutMapping("/journey/topics/{topicId}/primer")
    @PreAuthorize("hasRole('ADMIN')")
    fun updatePrimer(
        @PathVariable topicId: String,
        @RequestBody req: CurationPrimerUpdateRequest
    ): ResponseEntity<TopicPrimerDto> {
        return ResponseEntity.ok(topicPrimerService.updatePrimerByTopic(topicId, req))
    }

    @DeleteMapping("/journey/topics/{topicId}/primer")
    @PreAuthorize("hasRole('ADMIN')")
    fun deletePrimer(@PathVariable topicId: String): ResponseEntity<Map<String, String>> {
        topicPrimerService.deletePrimer(topicId)
        return ResponseEntity.ok(mapOf("status" to "deleted", "topicId" to topicId))
    }

    @GetMapping("/journey/continue")
    fun getContinue(): ResponseEntity<JourneyContinueDto> {
        return ResponseEntity.ok(journeyService.getContinue())
    }

    @GetMapping("/journey/subjects/{subjectId}/notebook")
    fun getSubjectNotebook(
        @PathVariable subjectId: String,
        @RequestParam(defaultValue = "false") starredOnly: Boolean,
        @RequestParam(required = false) topicId: String?
    ): ResponseEntity<SubjectNotebookDto> {
        return ResponseEntity.ok(journeyService.getSubjectNotebook(subjectId, starredOnly, topicId))
    }

    @GetMapping("/curation/primers")
    @PreAuthorize("hasRole('ADMIN')")
    fun getCurationPrimers(
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<TopicPrimerDto>> {
        return ResponseEntity.ok(topicPrimerService.getCurationDrafts(pageable))
    }

    @PatchMapping("/curation/primers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateCurationPrimer(
        @PathVariable id: UUID,
        @RequestBody req: CurationPrimerUpdateRequest
    ): ResponseEntity<TopicPrimerDto> {
        return ResponseEntity.ok(topicPrimerService.updateCuration(id, req))
    }

    @GetMapping("/journey/topics/{topicId}/concepts")
    fun getTopicConcepts(@PathVariable topicId: String): ResponseEntity<TopicConceptCoverageResponseDto> {
        return ResponseEntity.ok(conceptCoverageService.getTopicConceptCoverage(topicId))
    }

    @GetMapping("/curation/mappings/unmapped")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUnmappedQuestions(
        @RequestParam(required = false) topicId: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<UnmappedMappingQuestionDto>> {
        return ResponseEntity.ok(conceptCoverageService.getUnmappedQuestions(topicId, pageable))
    }

    @PatchMapping("/curation/questions/{questionId}/concepts")
    @PreAuthorize("hasRole('ADMIN')")
    fun patchQuestionConcepts(
        @PathVariable questionId: UUID,
        @RequestBody req: CurationQuestionConceptsPatchRequest
    ): ResponseEntity<QuestionDto> {
        return ResponseEntity.ok(conceptCoverageService.patchQuestionConcepts(questionId, req))
    }

    @GetMapping("/journey/concepts/{slug}/lesson")
    fun getConceptLesson(@PathVariable slug: String): ResponseEntity<ConceptLessonResponseDto> {
        return ResponseEntity.ok(conceptLessonService.getLesson(slug))
    }

    @PostMapping("/journey/concepts/{slug}/lesson/generate")
    fun generateConceptLesson(
        @PathVariable slug: String,
        @RequestParam(required = false) provider: String?,
        @RequestParam(required = false) model: String?,
        @RequestParam(defaultValue = "false") force: Boolean,
        @RequestHeader(value = "X-Api-Key", required = false) authHeaderKey: String?
    ): ResponseEntity<ConceptLessonResponseDto> {
        val userId = currentUserProvider.getCurrentUserId()
        val res = conceptLessonService.generateOrGetCached(slug, userId, provider, model, authHeaderKey, force)
        return ResponseEntity.ok(res)
    }

    @GetMapping("/curation/lessons")
    @PreAuthorize("hasRole('ADMIN')")
    fun getCurationLessons(
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<ConceptLessonDto>> {
        return ResponseEntity.ok(conceptLessonService.getCurationLessons(pageable))
    }

    @PutMapping("/curation/concepts/{slug}/lesson")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateConceptLesson(
        @PathVariable slug: String,
        @RequestBody req: CurationLessonUpdateRequest
    ): ResponseEntity<ConceptLessonDto> {
        return ResponseEntity.ok(conceptLessonService.updateLesson(slug, req))
    }

    @DeleteMapping("/curation/concepts/{slug}/lesson")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteConceptLesson(@PathVariable slug: String): ResponseEntity<Map<String, String>> {
        conceptLessonService.deleteLesson(slug)
        return ResponseEntity.ok(mapOf("status" to "deleted", "slug" to slug))
    }
}

