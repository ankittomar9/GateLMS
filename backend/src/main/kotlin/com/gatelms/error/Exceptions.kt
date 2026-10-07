package com.gatelms.error

class EntityNotFoundException(message: String) : RuntimeException(message)

class ValidationException(message: String, val fieldErrors: Map<String, String> = emptyMap()) : RuntimeException(message)

class ConflictException(message: String) : RuntimeException(message)

class InvalidCredentialsException(message: String = "Invalid credentials") : RuntimeException(message)

class RateLimitException(message: String, val retryAfterSeconds: Long) : RuntimeException(message)

class ForbiddenException(message: String = "Forbidden") : RuntimeException(message)
