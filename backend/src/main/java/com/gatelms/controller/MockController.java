package com.gatelms.controller;

import com.gatelms.dto.CreateMockTemplateRequest;
import com.gatelms.dto.MockAttemptHistoryDto;
import com.gatelms.dto.MockTemplateSummaryDto;
import com.gatelms.dto.StartMockAttemptResponse;
import com.gatelms.service.MockTestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mocks")
public class MockController {

    private final MockTestService mockTestService;

    public MockController(MockTestService mockTestService) {
        this.mockTestService = mockTestService;
    }

    @PostMapping
    public ResponseEntity<MockTemplateSummaryDto> createTemplate(@RequestBody CreateMockTemplateRequest req) {
        MockTemplateSummaryDto created = mockTestService.createTemplate(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/pyp/{year}")
    public ResponseEntity<MockTemplateSummaryDto> generatePypMock(@PathVariable int year) {
        MockTemplateSummaryDto generated = mockTestService.generatePypMock(year);
        return ResponseEntity.status(HttpStatus.CREATED).body(generated);
    }

    @GetMapping
    public List<MockTemplateSummaryDto> listTemplates() {
        return mockTestService.listTemplates();
    }

    @GetMapping("/{id}")
    public MockTemplateSummaryDto getTemplate(@PathVariable UUID id) {
        return mockTestService.getTemplate(id);
    }

    @PostMapping("/{id}/attempts")
    public StartMockAttemptResponse startAttempt(@PathVariable UUID id) {
        return mockTestService.startAttempt(id);
    }

    @GetMapping("/{id}/attempts")
    public List<MockAttemptHistoryDto> getAttemptHistory(@PathVariable UUID id) {
        return mockTestService.getAttemptHistory(id);
    }
}
