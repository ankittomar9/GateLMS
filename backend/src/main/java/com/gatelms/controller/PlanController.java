package com.gatelms.controller;

import com.gatelms.dto.*;
import com.gatelms.service.PlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping("/plans/generate")
    public ResponseEntity<StudyPlanDetailDto> generatePlan(@RequestBody GeneratePlanRequest req) {
        StudyPlanDetailDto plan = planService.generatePlan(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(plan);
    }

    @GetMapping("/plans")
    public ResponseEntity<List<StudyPlanSummaryDto>> getPlans() {
        List<StudyPlanSummaryDto> list = planService.getPlans();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/plans/today")
    public ResponseEntity<TodayPlanResponse> getTodayPlan() {
        TodayPlanResponse today = planService.getTodayPlan();
        return ResponseEntity.ok(today);
    }

    @GetMapping("/plans/{id}")
    public ResponseEntity<StudyPlanDetailDto> getPlanById(@PathVariable UUID id) {
        StudyPlanDetailDto plan = planService.getPlanById(id);
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/plans/{id}/regenerate")
    public ResponseEntity<StudyPlanDetailDto> regeneratePlan(@PathVariable UUID id) {
        StudyPlanDetailDto plan = planService.regeneratePlan(id);
        return ResponseEntity.ok(plan);
    }

    @PatchMapping("/plan-items/{id}")
    public ResponseEntity<PlanItemDto> updatePlanItemStatus(
            @PathVariable UUID id,
            @RequestBody UpdatePlanItemStatusRequest req
    ) {
        PlanItemDto item = planService.updateItemStatus(id, req);
        return ResponseEntity.ok(item);
    }
}
