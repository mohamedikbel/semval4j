package io.semval4j.core.exception;

/**
 * Base type for all SemVal4j-owned runtime exceptions.
 */
public class SemValException extends RuntimeException {

    public SemValException(String message) {
        super(message);
    }

    public SemValException(String message, Throwable cause) {
        super(message, cause);
    }
}
