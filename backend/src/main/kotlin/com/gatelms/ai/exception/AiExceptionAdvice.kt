package com.gatelms.ai.exception

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = ["com.gatelms.ai", "com.gatelms.anatomy", "com.gatelms.controller"])
class AiExceptionAdvice {

    @ExceptionHandler(AiValidationException::class)
    fun handleValidation(ex: AiValidationException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request").apply {
            title = "Bad Request"
            type = URI.create("about:blank")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem)
    }

    @ExceptionHandler(AiPayloadTooLargeException::class)
    fun handlePayloadTooLarge(ex: AiPayloadTooLargeException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.message ?: "Payload too large").apply {
            title = "Payload Too Large"
            type = URI.create("about:blank")
        }
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem)
    }

    @ExceptionHandler(AiRateLimitException::class)
    fun handleRateLimit(ex: AiRateLimitException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.message ?: "Rate limit exceeded").apply {
            title = "Too Many Requests"
            type = URI.create("about:blank")
            setProperty("retryAfter", ex.retryAfterSeconds)
        }
        val headers = HttpHeaders()
        headers.set(HttpHeaders.RETRY_AFTER, ex.retryAfterSeconds.toString())
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body(problem)
    }

    @ExceptionHandler(AiProviderException::class)
    fun handleProvider(ex: AiProviderException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.message ?: "AI provider error").apply {
            title = "Bad Gateway"
            type = URI.create("about:blank")
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem)
    }

    @ExceptionHandler(AiServiceUnavailableException::class)
    fun handleServiceUnavailable(ex: AiServiceUnavailableException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.message ?: "AI service unavailable").apply {
            title = "Service Unavailable"
            type = URI.create("about:blank")
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem)
    }

    @ExceptionHandler(AiTimeoutException::class)
    fun handleTimeout(ex: AiTimeoutException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, ex.message ?: "AI upstream timed out").apply {
            title = "Gateway Timeout"
            type = URI.create("about:blank")
        }
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(problem)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid argument").apply {
            title = "Bad Request"
            type = URI.create("about:blank")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem)
    }
}
