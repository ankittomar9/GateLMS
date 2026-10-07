package com.gatelms.dto

import jakarta.validation.constraints.NotBlank

data class AuthLoginRequest(
    @field:NotBlank(message = "Username must not be blank")
    val username: String = "",

    @field:NotBlank(message = "Password must not be blank")
    val password: String = ""
)

data class AuthMeResponse(
    val userId: String,
    val username: String,
    val displayName: String,
    val role: String
)

data class AuthStatusResponse(
    val status: String
)
