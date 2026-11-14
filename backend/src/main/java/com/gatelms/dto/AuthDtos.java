package com.gatelms.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthDtos {
    public record AuthLoginRequest(
        @NotBlank(message = "Username must not be blank")
        String username,

        @NotBlank(message = "Password must not be blank")
        String password
    ) {}

    public record AuthMeResponse(
        String userId,
        String username,
        String displayName,
        String role
    ) {}

    public record AuthStatusResponse(
        String status
    ) {}
}
