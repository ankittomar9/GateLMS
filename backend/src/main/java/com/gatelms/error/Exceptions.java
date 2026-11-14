package com.gatelms.error;

public class Exceptions {
    public static class EntityNotFoundException extends com.gatelms.error.EntityNotFoundException {
        public EntityNotFoundException(String message) { super(message); }
    }
    public static class ValidationException extends com.gatelms.error.ValidationException {
        public ValidationException(String message) { super(message); }
        public ValidationException(String message, java.util.Map<String, String> fieldErrors) { super(message, fieldErrors); }
    }
    public static class ConflictException extends com.gatelms.error.ConflictException {
        public ConflictException(String message) { super(message); }
    }
    public static class InvalidCredentialsException extends com.gatelms.error.InvalidCredentialsException {
        public InvalidCredentialsException(String message) { super(message); }
        public InvalidCredentialsException() { super(); }
    }
    public static class RateLimitException extends com.gatelms.error.RateLimitException {
        public RateLimitException(String message, long retryAfterSeconds) { super(message, retryAfterSeconds); }
    }
    public static class ForbiddenException extends com.gatelms.error.ForbiddenException {
        public ForbiddenException(String message) { super(message); }
        public ForbiddenException() { super(); }
    }
}
