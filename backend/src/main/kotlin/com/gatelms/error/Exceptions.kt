package com.gatelms.error

class EntityNotFoundException(message: String) : RuntimeException(message)

class ValidationException(message: String, val fieldErrors: Map<String, String> = emptyMap()) : RuntimeException(message)

class ConflictException(message: String) : RuntimeException(message)
