package com.gatelms.controller;

import com.gatelms.dto.ExamCountdownDto;
import com.gatelms.service.ExamCountdownService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exam")
public class ExamController {

    private final ExamCountdownService examCountdownService;

    public ExamController(ExamCountdownService examCountdownService) {
        this.examCountdownService = examCountdownService;
    }

    @GetMapping("/countdown")
    public ExamCountdownDto getCountdown() {
        return examCountdownService.getCountdown();
    }
}
