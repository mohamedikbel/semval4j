package io.semval4j.core.model;

import io.semval4j.core.exception.SemValConfigurationException;
import io.semval4j.core.policy.UncertainPolicy;

/**
 * Framework-neutral definition of a semantic validation rule.
 *
 * @param id        unique rule identifier, never blank
 * @param question  yes/no question evaluated by the Decision Model, never blank
 * @param property  property the violation is attached to, empty string for object level
 * @param passAt    probability at or above which the rule is {@link DecisionOutcome#VALID}, in [0, 1]
 * @param failBelow probability at or below which the rule is {@link DecisionOutcome#INVALID}, in [0, 1]
 * @param uncertain policy applied when the probability lies between the thresholds
 */
public record SemanticRuleDefinition(
        String id,
        String question,
        String property,
        double passAt,
        double failBelow,
        UncertainPolicy uncertain) {

    public static final double DEFAULT_PASS_AT = 0.85;
    public static final double DEFAULT_FAIL_BELOW = 0.20;

    public SemanticRuleDefinition {
        if (id == null || id.isBlank()) {
            throw new SemValConfigurationException("rule id must not be blank");
        }
        if (question == null || question.isBlank()) {
            throw new SemValConfigurationException("rule '" + id + "': question must not be blank");
        }
        if (passAt < 0.0 || passAt > 1.0) {
            throw new SemValConfigurationException("rule '" + id + "': passAt must be between 0 and 1, got " + passAt);
        }
        if (failBelow < 0.0 || failBelow > 1.0) {
            throw new SemValConfigurationException("rule '" + id + "': failBelow must be between 0 and 1, got " + failBelow);
        }
        if (failBelow >= passAt) {
            throw new SemValConfigurationException(
                    "rule '" + id + "': failBelow (" + failBelow + ") must be strictly lower than passAt (" + passAt + ")");
        }
        if (uncertain == null) {
            uncertain = UncertainPolicy.FAIL;
        }
        if (property == null) {
            property = "";
        }
    }

    /**
     * Creates a rule with default thresholds ({@code passAt=0.85}, {@code failBelow=0.20})
     * and the default {@link UncertainPolicy#FAIL} policy.
     */
    public static SemanticRuleDefinition of(String id, String question) {
        return new SemanticRuleDefinition(id, question, "", DEFAULT_PASS_AT, DEFAULT_FAIL_BELOW, UncertainPolicy.FAIL);
    }

    /**
     * @return the question to send to the Decision Model for this rule
     */
    public SemanticQuestion toQuestion() {
        return new SemanticQuestion(id, question);
    }

    /**
     * Maps a YES probability to an outcome using this rule's thresholds.
     */
    public DecisionOutcome outcomeFor(double probability) {
        if (probability >= passAt) {
            return DecisionOutcome.VALID;
        }
        if (probability <= failBelow) {
            return DecisionOutcome.INVALID;
        }
        return DecisionOutcome.UNCERTAIN;
    }
}
