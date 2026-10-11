package io.semval4j.core.exception;

/**
 * Raised when semantic validation is misconfigured: blank rule ids, duplicate
 * rule ids on one object, thresholds outside [0, 1], or {@code failBelow >= passAt}.
 */
public class SemValConfigurationException extends SemValException {

    public SemValConfigurationException(String message) {
        super(message);
    }

    public SemValConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
