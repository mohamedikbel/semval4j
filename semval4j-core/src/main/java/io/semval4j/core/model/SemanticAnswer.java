package io.semval4j.core.model;

/**
 * Provider answer for one question.
 *
 * @param id          identifier of the answered question
 * @param probability probability of YES in [0, 1]
 * @param outcome     outcome as classified by the provider, may be {@code null}
 *                    when the provider leaves classification to SemVal4j
 */
public record SemanticAnswer(String id, double probability, DecisionOutcome outcome) {

    public SemanticAnswer {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("answer id must not be blank");
        }
        if (probability < 0.0 || probability > 1.0 || Double.isNaN(probability)) {
            throw new IllegalArgumentException("probability must be between 0 and 1, got " + probability);
        }
    }
}
