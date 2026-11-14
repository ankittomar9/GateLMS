package com.gatelms.ai.exception;

public class AiExceptions {
    public static class AiException extends RuntimeException {
        public AiException(String message) { super(message); }
        public AiException(String message, Throwable cause) { super(message, cause); }
    }

    public static class AiValidationException extends AiException {
        public AiValidationException(String message) { super(message); }
    }

    public static class AiPayloadTooLargeException extends AiException {
        public AiPayloadTooLargeException(String message) { super(message); }
    }

    public static class AiRateLimitException extends AiException {
        private final long retryAfterSeconds;

        public AiRateLimitException(long retryAfterSeconds, String message) {
            super(message);
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() { return retryAfterSeconds; }
    }

    public static class AiProviderException extends AiException {
        public AiProviderException(String message) { super(message); }
        public AiProviderException(String message, Throwable cause) { super(message, cause); }
    }

    public static class AiServiceUnavailableException extends AiException {
        public AiServiceUnavailableException(String message) { super(message); }
        public AiServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
    }

    public static class AiTimeoutException extends AiException {
        public AiTimeoutException(String message) { super(message); }
        public AiTimeoutException(String message, Throwable cause) { super(message, cause); }
    }
}
