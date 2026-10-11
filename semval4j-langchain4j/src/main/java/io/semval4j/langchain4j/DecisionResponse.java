package io.semval4j.langchain4j;

import java.util.List;

/**
 * Batched Decision Model response.
 *
 * @param answers one answer per requested question
 */
public record DecisionResponse(List<DecisionAnswer> answers) {

    public DecisionResponse {
        answers = answers == null ? List.of() : List.copyOf(answers);
    }
}
