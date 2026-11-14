package com.gatelms.controller;

import com.gatelms.anatomy.dto.CardReviewRequest;
import com.gatelms.anatomy.dto.CardReviewResponse;
import com.gatelms.anatomy.dto.DueCardsResponse;
import com.gatelms.anatomy.service.CardSchedulingService;
import com.gatelms.dto.PracticeSessionResponse;
import com.gatelms.dto.RevisionDrillRequest;
import com.gatelms.dto.RevisionOverviewDto;
import com.gatelms.service.RevisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/revision")
public class RevisionController {

    private final RevisionService revisionService;
    private final CardSchedulingService cardSchedulingService;

    public RevisionController(
            RevisionService revisionService,
            CardSchedulingService cardSchedulingService
    ) {
        this.revisionService = revisionService;
        this.cardSchedulingService = cardSchedulingService;
    }

    @GetMapping("/overview")
    public ResponseEntity<RevisionOverviewDto> getOverview() {
        RevisionOverviewDto overview = revisionService.getOverview();
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/cards/due")
    public ResponseEntity<DueCardsResponse> getDueCards() {
        DueCardsResponse due = cardSchedulingService.getDueCards();
        return ResponseEntity.ok(due);
    }

    @PostMapping("/cards/{cardId}/review")
    public ResponseEntity<CardReviewResponse> reviewCard(
            @PathVariable UUID cardId,
            @RequestBody CardReviewRequest request
    ) {
        CardReviewResponse result = cardSchedulingService.reviewCard(cardId, request.grade());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/drills")
    public ResponseEntity<PracticeSessionResponse> createDrill(
            @RequestBody RevisionDrillRequest request
    ) {
        PracticeSessionResponse session = revisionService.createDrill(request);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/mistakes/{id}/redrill")
    public ResponseEntity<PracticeSessionResponse> redrillMistake(
            @PathVariable UUID id
    ) {
        PracticeSessionResponse session = revisionService.redrillMistake(id);
        return ResponseEntity.ok(session);
    }
}
