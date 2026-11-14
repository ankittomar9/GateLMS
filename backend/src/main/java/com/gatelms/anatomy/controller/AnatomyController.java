package com.gatelms.anatomy.controller;

import com.gatelms.anatomy.dto.AnatomyDtos.*;
import com.gatelms.anatomy.service.AnatomyGenerationService;
import com.gatelms.anatomy.service.ConceptService;
import com.gatelms.dto.QuestionDtos.QuestionSummaryDto;
import com.gatelms.entity.User;
import com.gatelms.entity.UserRole;
import com.gatelms.security.CurrentUserProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AnatomyController {

    public record AnatomyGenerateRequest(
        String provider,
        String model
    ) {}

    private final AnatomyGenerationService anatomyGenerationService;
    private final ConceptService conceptService;
    private final CurrentUserProvider currentUserProvider;

    public AnatomyController(
        AnatomyGenerationService anatomyGenerationService,
        ConceptService conceptService,
        CurrentUserProvider currentUserProvider
    ) {
        this.anatomyGenerationService = anatomyGenerationService;
        this.conceptService = conceptService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/questions/{id}/anatomy")
    public ResponseEntity<QuestionAnatomyDto> getAnatomy(@PathVariable UUID id) {
        User user = currentUserProvider.getCurrentUser();
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        QuestionAnatomyDto dto = anatomyGenerationService.getAnatomy(id, user.getId(), isAdmin);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/questions/{id}/anatomy/generate")
    public ResponseEntity<QuestionAnatomyDto> generateAnatomy(
        @PathVariable UUID id,
        @RequestParam(defaultValue = "false") boolean force,
        @RequestBody(required = false) AnatomyGenerateRequest req,
        @RequestHeader(value = "x-api-key", required = false) String authHeaderKey
    ) {
        User user = currentUserProvider.getCurrentUser();
        QuestionAnatomyDto dto = anatomyGenerationService.generateOrGetCached(
            id,
            user.getId(),
            req != null ? req.provider() : null,
            req != null ? req.model() : null,
            authHeaderKey,
            force
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/questions/{id}/anatomy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuestionAnatomyDto> deleteAnatomy(@PathVariable UUID id) {
        QuestionAnatomyDto dto = anatomyGenerationService.softDeleteAnatomy(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/concepts/{slug}")
    public ResponseEntity<ConceptUnitDto> getConcept(@PathVariable String slug) {
        ConceptUnitDto dto = conceptService.getConcept(slug);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/concepts/{slug}/generate")
    public ResponseEntity<ConceptUnitDto> generateConcept(
        @PathVariable String slug,
        @RequestBody(required = false) AnatomyGenerateRequest req,
        @RequestHeader(value = "x-api-key", required = false) String authHeaderKey
    ) {
        User user = currentUserProvider.getCurrentUser();
        ConceptUnitDto dto = conceptService.generateOrGetCached(
            slug,
            user.getId(),
            req != null ? req.provider() : null,
            req != null ? req.model() : null,
            authHeaderKey
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/concepts/{slug}/questions")
    public ResponseEntity<Page<QuestionSummaryDto>> getConceptQuestions(
        @PathVariable String slug,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<QuestionSummaryDto> page = conceptService.getConceptQuestions(slug, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/curation/anatomy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<QuestionAnatomyDto>> getCurationDrafts(
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<QuestionAnatomyDto> page = anatomyGenerationService.getCurationDrafts(pageable);
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/curation/anatomy/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuestionAnatomyDto> updateCuration(
        @PathVariable UUID id,
        @RequestBody CurationAnatomyUpdateRequest req
    ) {
        QuestionAnatomyDto dto = anatomyGenerationService.updateCuration(id, req);
        return ResponseEntity.ok(dto);
    }
}
