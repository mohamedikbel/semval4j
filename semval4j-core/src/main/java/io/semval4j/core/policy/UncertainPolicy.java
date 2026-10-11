package io.semval4j.core.policy;

/**
 * Defines what happens when a Decision Model probability falls inside the
 * uncertainty band (strictly between {@code failBelow} and {@code passAt}).
 */
public enum UncertainPolicy {

    /** Treat uncertainty as a validation failure. Default, safest option. */
    FAIL,

    /** Treat uncertainty as a pass. For permissive applications. */
    PASS,

    /** Throw {@code SemValUncertainException} instead of producing an outcome. */
    ERROR
}
