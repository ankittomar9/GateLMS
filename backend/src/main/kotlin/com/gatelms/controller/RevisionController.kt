package com.gatelms.controller

import com.gatelms.dto.PracticeSessionResponse
import com.gatelms.dto.RevisionDrillRequest
import com.gatelms.dto.RevisionOverviewDto
import com.gatelms.service.RevisionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/revision")
class RevisionController(
    private val revisionService: RevisionService
) {

    @GetMapping("/overview")
    fun getOverview(): ResponseEntity<RevisionOverviewDto> {
        val overview = revisionService.getOverview()
        return ResponseEntity.ok(overview)
    }

    @PostMapping("/drills")
    fun createDrill(
        @RequestBody request: RevisionDrillRequest
    ): ResponseEntity<PracticeSessionResponse> {
        val session = revisionService.createDrill(request)
        return ResponseEntity.ok(session)
    }

    @PostMapping("/mistakes/{id}/redrill")
    fun redrillMistake(
        @PathVariable id: UUID
    ): ResponseEntity<PracticeSessionResponse> {
        val session = revisionService.redrillMistake(id)
        return ResponseEntity.ok(session)
    }
}
