package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotePageResponse(
    List<NoteDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean isFirst,
    boolean isLast
) {}
