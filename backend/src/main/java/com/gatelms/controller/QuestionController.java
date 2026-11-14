package com.gatelms.controller;

import com.gatelms.dto.*;
import com.gatelms.entity.Pass1Status;
import com.gatelms.entity.Pass2Status;
import com.gatelms.error.ValidationException;
import com.gatelms.service.QuestionService;
import com.gatelms.service.SimilarQuestionsService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/questions")
public class QuestionController {

    private final QuestionService questionService;
    private final SimilarQuestionsService similarQuestionsService;

    public QuestionController(
            QuestionService questionService,
            SimilarQuestionsService similarQuestionsService
    ) {
        this.questionService = questionService;
        this.similarQuestionsService = similarQuestionsService;
    }

    @GetMapping("/{id}/similar")
    public SimilarQuestionsResponseDto getSimilarQuestions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "5") int limit
    ) {
        int boundedLimit = Math.min(20, Math.max(1, limit));
        return similarQuestionsService.getSimilarQuestions(id, boundedLimit);
    }

    @GetMapping
    public Page<QuestionSummaryDto> searchQuestions(
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String topicId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String pass1Status,
            @RequestParam(required = false) String pass2Status,
            @RequestParam(required = false) Boolean bookmarked,
            @RequestParam(required = false) String curationStatus,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "created_at") String sort,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Set<String> allowedSorts = Set.of("created_at", "year", "marks");
        String normalizedSort = (sort == null || sort.trim().isBlank()) ? "created_at" : sort.trim().toLowerCase();
        if (!allowedSorts.contains(normalizedSort)) {
            throw new ValidationException(
                    "Invalid sort parameter",
                    Map.of("sort", "Must be one of: created_at, year, marks")
            );
        }
        return questionService.searchQuestions(
                subjectId,
                topicId,
                type,
                year,
                pass1Status,
                pass2Status,
                bookmarked,
                curationStatus,
                source,
                difficulty,
                q,
                normalizedSort,
                pageable
        );
    }

    @GetMapping("/{id}")
    public QuestionDto getQuestionById(@PathVariable UUID id) {
        return questionService.getQuestionById(id);
    }

    @PostMapping("/validate")
    public QuestionValidationResultDto validateQuestion(@RequestBody QuestionUpsertDto dto) {
        return questionService.validateUpsert(dto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionDto createQuestion(@Valid @RequestBody QuestionUpsertDto dto) {
        return questionService.createQuestion(dto);
    }

    @PutMapping("/{id}")
    public QuestionDto updateQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody QuestionUpsertDto dto
    ) {
        return questionService.updateQuestion(id, dto);
    }

    @PatchMapping("/{id}/status")
    public QuestionDto updateStatus(
            @PathVariable UUID id,
            @RequestBody QuestionStatusUpdateDto dto
    ) {
        return questionService.updateStatus(id, dto);
    }

    @PatchMapping("/{id}/pass1")
    public QuestionDto updatePass1(
            @PathVariable UUID id,
            @RequestParam String status
    ) {
        Pass1Status normalized;
        String upper = status != null ? status.toUpperCase() : "";
        switch (upper) {
            case "UNSEEN" -> normalized = Pass1Status.PENDING;
            case "REVISE", "FLAGGED" -> normalized = Pass1Status.ASSISTED;
            case "SOLVED" -> normalized = Pass1Status.SOLVED;
            case "FAILED" -> normalized = Pass1Status.FAILED;
            default -> {
                try {
                    normalized = Pass1Status.valueOf(upper);
                } catch (Exception e) {
                    normalized = Pass1Status.PENDING;
                }
            }
        }
        return questionService.updateStatus(id, new QuestionStatusUpdateDto(normalized, null, null));
    }

    @PatchMapping("/{id}/pass2")
    public QuestionDto updatePass2(
            @PathVariable UUID id,
            @RequestParam String status
    ) {
        Pass2Status normalized;
        String upper = status != null ? status.toUpperCase() : "";
        switch (upper) {
            case "NOT_ATTEMPTED", "TIMEOUT" -> normalized = Pass2Status.PENDING;
            case "SOLVED" -> normalized = Pass2Status.SOLVED;
            case "FAILED" -> normalized = Pass2Status.FAILED;
            default -> {
                try {
                    normalized = Pass2Status.valueOf(upper);
                } catch (Exception e) {
                    normalized = Pass2Status.PENDING;
                }
            }
        }
        return questionService.updateStatus(id, new QuestionStatusUpdateDto(null, normalized, null));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/curation")
    public QuestionDto updateCuration(
            @PathVariable UUID id,
            @RequestBody CurationUpdateRequest dto
    ) {
        return questionService.updateCuration(id, dto);
    }

    @PatchMapping("/{id}/bookmark")
    public QuestionDto updateBookmark(
            @PathVariable UUID id,
            @RequestBody BookmarkRequest request
    ) {
        return questionService.updateBookmark(id, request.bookmarked());
    }
}
