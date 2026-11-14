package com.gatelms.dto;

import java.util.List;

public record SeedInventoryResponse(
    long totalQuestions,
    long totalVisibleQuestions,
    List<SubjectInventoryDto> subjects
) {}
