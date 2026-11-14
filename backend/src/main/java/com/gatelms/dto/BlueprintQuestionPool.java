package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BlueprintQuestionPool(
    List<String> curationStatus,
    List<String> difficulty,
    Integer year,
    String source
) {
    public BlueprintQuestionPool() { this(List.of("ACCEPTED", "UNREVIEWED"), null, null, null); }
}
