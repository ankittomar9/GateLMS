package com.gatelms.anatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.*;

public record CardReviewResponse(
    Instant nextDueAt,
    float intervalDays,
    float ease
) {}
