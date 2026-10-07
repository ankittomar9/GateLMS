package com.gatelms.controller

import com.gatelms.dto.*
import com.gatelms.service.PlanService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class PlanController(
    private val planService: PlanService
) {

    @PostMapping("/plans/generate")
    fun generatePlan(@RequestBody req: GeneratePlanRequest): ResponseEntity<StudyPlanDetailDto> {
        val plan = planService.generatePlan(req)
        return ResponseEntity.status(HttpStatus.CREATED).body(plan)
    }

    @GetMapping("/plans")
    fun getPlans(): ResponseEntity<List<StudyPlanSummaryDto>> {
        val list = planService.getPlans()
        return ResponseEntity.ok(list)
    }

    @GetMapping("/plans/today")
    fun getTodayPlan(): ResponseEntity<TodayPlanResponse> {
        val today = planService.getTodayPlan()
        return ResponseEntity.ok(today)
    }

    @GetMapping("/plans/{id}")
    fun getPlanById(@PathVariable id: UUID): ResponseEntity<StudyPlanDetailDto> {
        val plan = planService.getPlanById(id)
        return ResponseEntity.ok(plan)
    }

    @PostMapping("/plans/{id}/regenerate")
    fun regeneratePlan(@PathVariable id: UUID): ResponseEntity<StudyPlanDetailDto> {
        val plan = planService.regeneratePlan(id)
        return ResponseEntity.ok(plan)
    }

    @PatchMapping("/plan-items/{id}")
    fun updatePlanItemStatus(
        @PathVariable id: UUID,
        @RequestBody req: UpdatePlanItemStatusRequest
    ): ResponseEntity<PlanItemDto> {
        val item = planService.updateItemStatus(id, req)
        return ResponseEntity.ok(item)
    }
}
