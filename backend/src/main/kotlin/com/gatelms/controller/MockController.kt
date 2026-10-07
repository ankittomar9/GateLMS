package com.gatelms.controller

import com.gatelms.dto.CreateMockTemplateRequest
import com.gatelms.dto.MockAttemptHistoryDto
import com.gatelms.dto.MockTemplateSummaryDto
import com.gatelms.dto.StartMockAttemptResponse
import com.gatelms.service.MockTestService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/mocks")
class MockController(
    private val mockTestService: MockTestService
) {

    @PostMapping
    fun createTemplate(@RequestBody req: CreateMockTemplateRequest): ResponseEntity<MockTemplateSummaryDto> {
        val created = mockTestService.createTemplate(req)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping
    fun listTemplates(): List<MockTemplateSummaryDto> {
        return mockTestService.listTemplates()
    }

    @GetMapping("/{id}")
    fun getTemplate(@PathVariable id: UUID): MockTemplateSummaryDto {
        return mockTestService.getTemplate(id)
    }

    @PostMapping("/{id}/attempts")
    fun startAttempt(@PathVariable id: UUID): StartMockAttemptResponse {
        return mockTestService.startAttempt(id)
    }

    @GetMapping("/{id}/attempts")
    fun getAttemptHistory(@PathVariable id: UUID): List<MockAttemptHistoryDto> {
        return mockTestService.getAttemptHistory(id)
    }
}
