package com.gatelms.ai.exception;

import com.gatelms.ai.exception.AiExceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.net.URI;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class AiExceptionAdvice {
    private static final Logger log = LoggerFactory.getLogger(AiExceptionAdvice.class);

    @ExceptionHandler(AiValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidation(AiValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setType(URI.create("https://gatelms.dev/errors/ai-validation"));
        problem.setTitle("AI Request Validation Error");
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(AiPayloadTooLargeException.class)
    public ResponseEntity<ProblemDetail> handlePayloadTooLarge(AiPayloadTooLargeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, ex.getMessage());
        problem.setType(URI.create("https://gatelms.dev/errors/ai-payload-too-large"));
        problem.setTitle("Audio Payload Too Large");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problem);
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<ProblemDetail> handleProvider(AiProviderException ex) {
        log.warn("AI Provider Exception: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        problem.setType(URI.create("https://gatelms.dev/errors/ai-provider-error"));
        problem.setTitle("AI Provider Error");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleServiceUnavailable(AiServiceUnavailableException ex) {
        log.warn("AI Service Unavailable: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setType(URI.create("https://gatelms.dev/errors/ai-service-unavailable"));
        problem.setTitle("AI Service Unavailable");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
    }

    @ExceptionHandler(AiTimeoutException.class)
    public ResponseEntity<ProblemDetail> handleTimeout(AiTimeoutException ex) {
        log.warn("AI Timeout: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
        problem.setType(URI.create("https://gatelms.dev/errors/ai-timeout"));
        problem.setTitle("AI Request Timeout");
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(problem);
    }
}
