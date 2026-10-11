package io.semval4j.core.model;

/**
 * A single yes/no question sent to a Decision Model.
 *
 * @param id       unique identifier correlating the answer with the rule, never blank
 * @param question natural language question evaluated against the state, never blank
 */
public record SemanticQuestion(String id, String question) {

    public SemanticQuestion {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("question id must not be blank");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question text must not be blank");
        }
    }
}
