package com.gatelms.controller

import com.gatelms.dto.MistakeCreateRequest
import com.gatelms.dto.MistakeDto
import com.gatelms.dto.MistakeReviewRequest
import com.gatelms.service.MistakeService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/mistakes")
class MistakeController(
    private val mistakeService: MistakeService
) {

    @PostMapping
    fun createMistake(
        @RequestBody request: MistakeCreateRequest
    ): ResponseEntity<MistakeDto> {
        val created = mistakeService.createMistake(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping
    fun getMistakes(
        @RequestParam(required = false) resolved: Boolean?,
        @RequestParam(defaultValue = "false") due: Boolean,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<MistakeDto>> {
        val page = mistakeService.getMistakes(resolved = resolved, dueOnly = due, pageable = pageable)
        return ResponseEntity.ok(page)
    }

    @PatchMapping("/{id}/review")
    fun reviewMistake(
        @PathVariable id: UUID,
        @RequestBody request: MistakeReviewRequest
    ): ResponseEntity<MistakeDto> {
        val updated = mistakeService.reviewMistake(id, request)
        return ResponseEntity.ok(updated)
    }
}
