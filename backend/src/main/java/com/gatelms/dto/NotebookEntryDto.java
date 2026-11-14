package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotebookEntryDto(
    UUID questionId,
    Integer year,
    String source,
    String statementSnippet,
    Instant solvedAt,
    boolean starred,
    boolean anatomyAvailable
) {}
