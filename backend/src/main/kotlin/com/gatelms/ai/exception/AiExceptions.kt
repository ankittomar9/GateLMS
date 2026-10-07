package com.gatelms.ai.exception

sealed class AiException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class AiValidationException(message: String) : AiException(message)

class AiPayloadTooLargeException(message: String) : AiException(message)

class AiRateLimitException(val retryAfterSeconds: Long, message: String) : AiException(message)

class AiProviderException(message: String, cause: Throwable? = null) : AiException(message, cause)

class AiServiceUnavailableException(message: String, cause: Throwable? = null) : AiException(message, cause)

class AiTimeoutException(message: String, cause: Throwable? = null) : AiException(message, cause)
