package com.gatelms.controller;

import com.gatelms.dto.SubjectDetailDto;
import com.gatelms.dto.SubjectSummaryDto;
import com.gatelms.service.SubjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    @GetMapping
    public List<SubjectSummaryDto> getAllSubjects() {
        return subjectService.getAllSubjects();
    }

    @GetMapping("/{idOrSlug}")
    public SubjectDetailDto getSubject(@PathVariable String idOrSlug) {
        return subjectService.getSubjectByIdOrSlug(idOrSlug);
    }
}
