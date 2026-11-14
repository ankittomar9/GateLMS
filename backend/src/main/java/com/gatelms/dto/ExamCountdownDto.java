package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExamCountdownDto(
    String examDate,
    long daysRemaining,
    long weeksRemaining,
    String source
) {
    public ExamCountdownDto(String examDate, long daysRemaining, long weeksRemaining) {
        this(examDate, daysRemaining, weeksRemaining, "config");
    }
}
