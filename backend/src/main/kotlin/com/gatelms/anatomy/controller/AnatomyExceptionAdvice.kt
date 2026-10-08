package com.gatelms.anatomy.controller

import com.gatelms.anatomy.exception.AnatomyLockedException
import com.gatelms.anatomy.exception.AnatomyNotFoundException
import com.gatelms.anatomy.exception.AnatomyValidationException
import com.gatelms.anatomy.exception.NoSourceQuestionsException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.net.URI

@RestControllerAdvice(basePackages = ["com.gatelms.anatomy"])
class AnatomyExceptionAdvice {

    @ExceptionHandler(AnatomyLockedException::class)
    fun handleLocked(ex: AnatomyLockedException): ResponseEntity<Map<String, String>> {
        val body = mapOf(
            "code" to ex.code,
            "message" to (ex.message ?: "Anatomy dossier is locked until you submit at least one attempt on this question.")
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body)
    }

    @ExceptionHandler(NoSourceQuestionsException::class)
    fun handleNoSource(ex: NoSourceQuestionsException): ResponseEntity<Map<String, String>> {
        val body = mapOf(
            "code" to ex.code,
            "message" to (ex.message ?: "At least one tagged question is required to generate a concept unit.")
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body)
    }

    @ExceptionHandler(AnatomyNotFoundException::class)
    fun handleNotFound(ex: AnatomyNotFoundException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found").apply {
            title = "Not Found"
            type = URI.create("about:blank")
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem)
    }

    @ExceptionHandler(AnatomyValidationException::class)
    fun handleValidation(ex: AnatomyValidationException): ResponseEntity<ProblemDetail> {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Validation error").apply {
            title = "Validation Error"
            type = URI.create("about:blank")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem)
    }
}
