package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PracticeSessionCreateRequest(
    String subjectId,
    String topicId,
    QuestionType questionType,
    Integer marks,
    String curationStatus,
    int count,
    Integer year
) {
    public PracticeSessionCreateRequest() { this(null, null, null, null, null, 10, null); }
}
