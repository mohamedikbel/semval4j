package io.semval4j.core.model;

/**
 * Probabilistic outcome of a semantic rule.
 *
 * <pre>
 * 0.00 -------- failBelow -------- passAt -------- 1.00
 *       INVALID       UNCERTAIN        VALID
 * </pre>
 */
public enum DecisionOutcome {
    /** Probability at or above the rule's {@code passAt} threshold. */
    VALID,
    /** Probability strictly between {@code failBelow} and {@code passAt}; the rule's {@code UncertainPolicy} decides the outcome. */
    UNCERTAIN,
    /** Probability at or below the rule's {@code failBelow} threshold. */
    INVALID
}
