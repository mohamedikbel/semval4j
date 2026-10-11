package io.semval4j.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Raw, provider-neutral result of one batched Decision Model call.
 *
 * @param answers  one answer per question, never {@code null}
 * @param provider name of the provider that produced the result, may be {@code null}
 * @param model    identifier of the decision model, may be {@code null}
 */
public record SemanticDecisionResult(List<SemanticAnswer> answers, String provider, String model) {

    public SemanticDecisionResult {
        Objects.requireNonNull(answers, "answers must not be null");
        answers = List.copyOf(answers);
    }
}
