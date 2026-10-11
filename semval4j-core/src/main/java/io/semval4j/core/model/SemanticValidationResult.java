package io.semval4j.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Aggregated result of validating one object against all its semantic rules.
 */
public record SemanticValidationResult(List<SemanticRuleResult> results) {

    public SemanticValidationResult {
        Objects.requireNonNull(results, "results must not be null");
        results = List.copyOf(results);
    }

    /** An empty result, used when an object declares no semantic rules. */
    public static SemanticValidationResult empty() {
        return new SemanticValidationResult(List.of());
    }

    /**
     * @return {@code true} when every rule passed (or there are no rules)
     */
    public boolean isValid() {
        return results.stream().allMatch(SemanticRuleResult::passed);
    }

    /**
     * @return the results of the rules that did not pass
     */
    public List<SemanticRuleResult> failures() {
        return results.stream().filter(r -> !r.passed()).toList();
    }
}
