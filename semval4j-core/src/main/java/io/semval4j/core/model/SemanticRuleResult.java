package io.semval4j.core.model;

import java.util.Objects;

/**
 * Evaluated outcome of one semantic rule, after threshold classification and
 * application of the {@code UncertainPolicy}.
 *
 * @param rule        the rule that was evaluated
 * @param probability YES probability returned by the provider
 * @param outcome     threshold classification of the probability
 * @param passed      whether the rule passed after applying the uncertainty policy
 */
public record SemanticRuleResult(
        SemanticRuleDefinition rule,
        double probability,
        DecisionOutcome outcome,
        boolean passed) {

    public SemanticRuleResult {
        Objects.requireNonNull(rule, "rule must not be null");
        Objects.requireNonNull(outcome, "outcome must not be null");
    }
}
