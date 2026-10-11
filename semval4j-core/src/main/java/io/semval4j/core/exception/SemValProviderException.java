package io.semval4j.core.exception;

/**
 * Raised when a Decision Model provider call fails or returns an unusable
 * answer. Provider SDK exceptions are always wrapped in this type so that
 * application code never sees provider-specific classes.
 */
public class SemValProviderException extends SemValException {

    public SemValProviderException(String message) {
        super(message);
    }

    public SemValProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
