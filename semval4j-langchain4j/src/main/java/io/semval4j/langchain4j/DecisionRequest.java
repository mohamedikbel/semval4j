package io.semval4j.langchain4j;

import java.util.List;
import java.util.Map;

/**
 * A single batched decision request: one state, many questions.
 *
 * @param model     decision model identifier, may be {@code null} for the provider default
 * @param state     serialized object under validation
 * @param questions all questions evaluated against the state in one call
 */
public record DecisionRequest(String model, Map<String, Object> state, List<DecisionQuestion> questions) {

    public DecisionRequest {
        state = state == null ? Map.of() : Map.copyOf(state);
        questions = questions == null ? List.of() : List.copyOf(questions);
    }
}
