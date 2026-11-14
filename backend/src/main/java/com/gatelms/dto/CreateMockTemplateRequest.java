package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMockTemplateRequest(
    String title,
    int durationMinutes,
    Double totalMarks,
    List<BlueprintSection> sections,
    BlueprintQuestionPool questionPool,
    boolean shuffle
) {
    public CreateMockTemplateRequest(String title) { this(title, 180, null, List.of(), null, true); }
}
