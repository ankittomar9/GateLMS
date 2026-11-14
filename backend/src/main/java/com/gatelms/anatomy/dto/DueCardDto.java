package com.gatelms.anatomy.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gatelms.anatomy.entity.*;

public record DueCardDto(
    UUID cardId,
    String prompt,
    String answer,
    String conceptSlug,
    String conceptLabel,
    String unitTitle,
    RetrievalCardStatus status
) {}
