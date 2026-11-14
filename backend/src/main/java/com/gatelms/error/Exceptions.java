package com.gatelms.error;

import java.util.Collections;
import java.util.Map;

public class Exceptions {
    public static class EntityNotFoundException extends RuntimeException {
        public EntityNotFoundException(String message) { super(message); }
    }

    public static class ValidationException extends RuntimeException {
        private final Map<String, String> fieldErrors;

        public ValidationException(String message) {
            this(message, Collections.emptyMap());
        }

        public ValidationException(String message, Map<String, String> fieldErrors) {
            super(message);
            this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyMap();
        }

        public Map<String, String> getFieldErrors() { return fieldErrors; }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) { super(message); }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) { super(message); }
        public InvalidCredentialsException() { super("Invalid credentials"); }
    }

    public static class RateLimitException extends RuntimeException {
        private final long retryAfterSeconds;

        public RateLimitException(String message, long retryAfterSeconds) {
            super(message);
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() { return retryAfterSeconds; }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
        public ForbiddenException() { super("Forbidden"); }
    }
}
