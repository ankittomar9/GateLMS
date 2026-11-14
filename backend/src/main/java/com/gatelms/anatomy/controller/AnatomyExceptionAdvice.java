package com.gatelms.anatomy.controller;

import com.gatelms.anatomy.exception.AnatomyExceptions.AnatomyLockedException;
import com.gatelms.anatomy.exception.AnatomyExceptions.AnatomyNotFoundException;
import com.gatelms.anatomy.exception.AnatomyExceptions.AnatomyValidationException;
import com.gatelms.anatomy.exception.AnatomyExceptions.NoSourceQuestionsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.gatelms.anatomy")
public class AnatomyExceptionAdvice {

    @ExceptionHandler(AnatomyLockedException.class)
    public ResponseEntity<Map<String, String>> handleLocked(AnatomyLockedException ex) {
        Map<String, String> body = Map.of(
            "code", ex.getCode(),
            "message", ex.getMessage() != null ? ex.getMessage() : "Anatomy dossier is locked until you submit at least one attempt on this question."
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(NoSourceQuestionsException.class)
    public ResponseEntity<Map<String, String>> handleNoSource(NoSourceQuestionsException ex) {
        Map<String, String> body = Map.of(
            "code", ex.getCode(),
            "message", ex.getMessage() != null ? ex.getMessage() : "At least one tagged question is required to generate a concept unit."
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(AnatomyNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(AnatomyNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage() != null ? ex.getMessage() : "Resource not found"
        );
        problem.setTitle("Not Found");
        problem.setType(URI.create("about:blank"));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(AnatomyValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(AnatomyValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage() != null ? ex.getMessage() : "Validation error"
        );
        problem.setTitle("Validation Error");
        problem.setType(URI.create("about:blank"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }
}
