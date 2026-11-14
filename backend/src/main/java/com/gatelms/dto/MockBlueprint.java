package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MockBlueprint(
    String title,
    Integer year,
    String source,
    List<BlueprintSection> sections,
    BlueprintQuestionPool questionPool,
    boolean shuffle
) {
    public MockBlueprint() { this(null, null, null, List.of(), null, true); }
}
