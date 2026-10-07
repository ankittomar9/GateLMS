package com.gatelms.error

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request content")
        problem.type = URI.create("https://gatelms.dev/errors/validation-failed")
        val fieldErrors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        problem.setProperty("fieldErrors", fieldErrors)
        return problem
    }

    @ExceptionHandler(ValidationException::class)
    fun handleValidationException(ex: ValidationException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Validation error")
        problem.type = URI.create("https://gatelms.dev/errors/validation-failed")
        if (ex.fieldErrors.isNotEmpty()) {
            problem.setProperty("fieldErrors", ex.fieldErrors)
        }
        return problem
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFound(ex: EntityNotFoundException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Entity not found")
        problem.type = URI.create("https://gatelms.dev/errors/not-found")
        return problem
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException::class, ConflictException::class)
    fun handleOptimisticLocking(ex: Exception): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.message ?: "Resource was modified concurrently. Please reload latest state."
        )
        problem.type = URI.create("https://gatelms.dev/errors/version-conflict")
        return problem
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException::class)
    fun handleResponseStatusException(ex: org.springframework.web.server.ResponseStatusException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(ex.statusCode, ex.reason ?: "Request error")
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ProblemDetail {
        logger.error("Unhandled server exception: {}", ex.message, ex)
        val problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected internal error occurred"
        )
        problem.type = URI.create("https://gatelms.dev/errors/internal-server-error")
        return problem
    }
}
