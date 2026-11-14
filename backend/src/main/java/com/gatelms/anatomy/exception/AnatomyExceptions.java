package com.gatelms.anatomy.exception;

public class AnatomyExceptions {
    public static class AnatomyValidationException extends RuntimeException {
        public AnatomyValidationException(String message) { super(message); }
    }

    public static class AnatomyLockedException extends RuntimeException {
        private final String code;

        public AnatomyLockedException(String code, String message) {
            super(message);
            this.code = code;
        }

        public AnatomyLockedException() {
            this("ANATOMY_LOCKED_UNTIL_ATTEMPT", "Anatomy dossier is locked until you submit at least one attempt on this question.");
        }

        public String getCode() { return code; }
    }

    public static class NoSourceQuestionsException extends RuntimeException {
        private final String code;

        public NoSourceQuestionsException(String code, String message) {
            super(message);
            this.code = code;
        }

        public NoSourceQuestionsException(String message) {
            this("NO_SOURCE_QUESTIONS", message);
        }

        public NoSourceQuestionsException() {
            this("NO_SOURCE_QUESTIONS", "At least one tagged question is required to generate a concept unit.");
        }

        public String getCode() { return code; }
    }

    public static class AnatomyNotFoundException extends RuntimeException {
        public AnatomyNotFoundException(String message) { super(message); }
    }
}
