package com.gatelms.controller;

import com.gatelms.anatomy.dto.*;
import com.gatelms.anatomy.service.ConceptCoverageService;
import com.gatelms.anatomy.service.ConceptLessonService;
import com.gatelms.anatomy.service.TopicPrimerService;
import com.gatelms.dto.*;
import com.gatelms.entity.UserRole;
import com.gatelms.security.CurrentUserProvider;
import com.gatelms.service.JourneyService;
import com.gatelms.service.TopicWorkbookPdfService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class JourneyController {

    private final JourneyService journeyService;
    private final TopicPrimerService topicPrimerService;
    private final CurrentUserProvider currentUserProvider;
    private final ConceptCoverageService conceptCoverageService;
    private final ConceptLessonService conceptLessonService;
    private final TopicWorkbookPdfService topicWorkbookPdfService;

    public JourneyController(
            JourneyService journeyService,
            TopicPrimerService topicPrimerService,
            CurrentUserProvider currentUserProvider,
            ConceptCoverageService conceptCoverageService,
            ConceptLessonService conceptLessonService,
            TopicWorkbookPdfService topicWorkbookPdfService
    ) {
        this.journeyService = journeyService;
        this.topicPrimerService = topicPrimerService;
        this.currentUserProvider = currentUserProvider;
        this.conceptCoverageService = conceptCoverageService;
        this.conceptLessonService = conceptLessonService;
        this.topicWorkbookPdfService = topicWorkbookPdfService;
    }

    @GetMapping("/journey/overview")
    public ResponseEntity<List<JourneyOverviewSubjectDto>> getOverview() {
        return ResponseEntity.ok(journeyService.getOverview());
    }

    @GetMapping("/journey/subjects/{subjectId}/topics")
    public ResponseEntity<List<JourneyTopicSummaryDto>> getSubjectTopics(@PathVariable String subjectId) {
        return ResponseEntity.ok(journeyService.getSubjectTopics(subjectId));
    }

    @GetMapping("/journey/topics/{topicId}/primer")
    public ResponseEntity<TopicPrimerResponseDto> getPrimer(@PathVariable String topicId) {
        return ResponseEntity.ok(topicPrimerService.getPrimer(topicId));
    }

    @PostMapping("/journey/topics/{topicId}/primer/generate")
    public ResponseEntity<TopicPrimerResponseDto> generatePrimer(
            @PathVariable String topicId,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model,
            @RequestParam(defaultValue = "false") boolean force,
            @RequestHeader(value = "X-Api-Key", required = false) String authHeaderKey
    ) {
        if (force && currentUserProvider.getCurrentUser().getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("force=true requires ADMIN");
        }
        UUID userId = currentUserProvider.getCurrentUserId();
        TopicPrimerResponseDto res = topicPrimerService.generateOrGetCached(topicId, userId, provider, model, authHeaderKey, force);
        return ResponseEntity.ok(res);
    }

    @PutMapping("/journey/topics/{topicId}/primer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TopicPrimerDto> updatePrimer(
            @PathVariable String topicId,
            @RequestBody CurationPrimerUpdateRequest req
    ) {
        return ResponseEntity.ok(topicPrimerService.updatePrimerByTopic(topicId, req));
    }

    @DeleteMapping("/journey/topics/{topicId}/primer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deletePrimer(@PathVariable String topicId) {
        topicPrimerService.deletePrimer(topicId);
        return ResponseEntity.ok(Map.of("status", "deleted", "topicId", topicId));
    }

    @GetMapping(value = "/journey/topics/{topicId}/workbook/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadWorkbookPdf(@PathVariable String topicId) {
        if (topicWorkbookPdfService == null) {
            throw new IllegalStateException("TopicWorkbookPdfService not available");
        }
        TopicWorkbookPdfService.WorkbookResult result = topicWorkbookPdfService.generateWorkbookPdf(topicId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(result.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.noStore())
                .body(result.content());
    }

    @GetMapping("/journey/continue")
    public ResponseEntity<JourneyContinueDto> getContinue() {
        return ResponseEntity.ok(journeyService.getContinue());
    }

    @GetMapping("/journey/subjects/{subjectId}/notebook")
    public ResponseEntity<SubjectNotebookDto> getSubjectNotebook(
            @PathVariable String subjectId,
            @RequestParam(defaultValue = "false") boolean starredOnly,
            @RequestParam(required = false) String topicId
    ) {
        return ResponseEntity.ok(journeyService.getSubjectNotebook(subjectId, starredOnly, topicId));
    }

    @GetMapping("/curation/primers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<TopicPrimerDto>> getCurationPrimers(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(topicPrimerService.getCurationDrafts(pageable));
    }

    @PatchMapping("/curation/primers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TopicPrimerDto> updateCurationPrimer(
            @PathVariable UUID id,
            @RequestBody CurationPrimerUpdateRequest req
    ) {
        return ResponseEntity.ok(topicPrimerService.updateCuration(id, req));
    }

    @GetMapping("/journey/topics/{topicId}/concepts")
    public ResponseEntity<TopicConceptCoverageResponseDto> getTopicConcepts(@PathVariable String topicId) {
        return ResponseEntity.ok(conceptCoverageService.getTopicConceptCoverage(topicId));
    }

    @GetMapping("/curation/mappings/unmapped")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UnmappedMappingQuestionDto>> getUnmappedQuestions(
            @RequestParam(required = false) String topicId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(conceptCoverageService.getUnmappedQuestions(topicId, pageable));
    }

    @PatchMapping("/curation/questions/{questionId}/concepts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuestionDto> patchQuestionConcepts(
            @PathVariable UUID questionId,
            @RequestBody CurationQuestionConceptsPatchRequest req
    ) {
        return ResponseEntity.ok(conceptCoverageService.patchQuestionConcepts(questionId, req));
    }

    @GetMapping("/journey/concepts/{slug}/lesson")
    public ResponseEntity<ConceptLessonResponseDto> getConceptLesson(@PathVariable String slug) {
        return ResponseEntity.ok(conceptLessonService.getLesson(slug));
    }

    @PostMapping("/journey/concepts/{slug}/lesson/generate")
    public ResponseEntity<ConceptLessonResponseDto> generateConceptLesson(
            @PathVariable String slug,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String model,
            @RequestParam(defaultValue = "false") boolean force,
            @RequestHeader(value = "X-Api-Key", required = false) String authHeaderKey
    ) {
        if (force && currentUserProvider.getCurrentUser().getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("force=true requires ADMIN");
        }
        UUID userId = currentUserProvider.getCurrentUserId();
        ConceptLessonResponseDto res = conceptLessonService.generateOrGetCached(slug, userId, provider, model, authHeaderKey, force);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/curation/lessons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ConceptLessonDto>> getCurationLessons(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(conceptLessonService.getCurationLessons(pageable));
    }

    @PutMapping("/curation/concepts/{slug}/lesson")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConceptLessonDto> updateConceptLesson(
            @PathVariable String slug,
            @RequestBody CurationLessonUpdateRequest req
    ) {
        return ResponseEntity.ok(conceptLessonService.updateLesson(slug, req));
    }

    @DeleteMapping("/curation/concepts/{slug}/lesson")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteConceptLesson(@PathVariable String slug) {
        conceptLessonService.deleteLesson(slug);
        return ResponseEntity.ok(Map.of("status", "deleted", "slug", slug));
    }
}
