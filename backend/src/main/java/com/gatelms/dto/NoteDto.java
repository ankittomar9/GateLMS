package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NoteDto(
    UUID id,
    String title,
    String filePath,
    String originalFilename,
    String contentType,
    Long fileSizeBytes,
    String contentMd,
    String subjectId,
    String subjectName,
    String topicId,
    String topicName,
    boolean isPinned,
    String kind,
    int version,
    Instant createdAt,
    Instant updatedAt
) {}
