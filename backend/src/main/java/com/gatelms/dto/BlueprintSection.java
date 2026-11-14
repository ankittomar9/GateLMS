package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BlueprintSection(
    String subjectId,
    int mcq,
    int msq,
    int nat,
    Integer marks
) {
    public BlueprintSection() { this("", 0, 0, 0, null); }
}
