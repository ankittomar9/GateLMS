package com.gatelms.controller;

import com.gatelms.dto.MistakeCreateRequest;
import com.gatelms.dto.MistakeDto;
import com.gatelms.dto.MistakeReviewRequest;
import com.gatelms.service.MistakeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mistakes")
public class MistakeController {

    private final MistakeService mistakeService;

    public MistakeController(MistakeService mistakeService) {
        this.mistakeService = mistakeService;
    }

    @PostMapping
    public ResponseEntity<MistakeDto> createMistake(@RequestBody MistakeCreateRequest request) {
        MistakeDto created = mistakeService.createMistake(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<MistakeDto>> getMistakes(
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "false") boolean due,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<MistakeDto> page = mistakeService.getMistakes(resolved, due, pageable);
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/{id}/review")
    public ResponseEntity<MistakeDto> reviewMistake(
            @PathVariable UUID id,
            @RequestBody MistakeReviewRequest request
    ) {
        MistakeDto updated = mistakeService.reviewMistake(id, request);
        return ResponseEntity.ok(updated);
    }
}
