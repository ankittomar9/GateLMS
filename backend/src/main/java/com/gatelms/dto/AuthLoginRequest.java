package com.gatelms.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.gatelms.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuthLoginRequest(
    @NotBlank(message = "Username must not be blank") String username,
    @NotBlank(message = "Password must not be blank") String password
) {}
