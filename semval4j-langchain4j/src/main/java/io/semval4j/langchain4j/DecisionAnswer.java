package io.semval4j.langchain4j;

/**
 * Decision Model answer for one question.
 *
 * @param id             question identifier
 * @param yesProbability probability that the answer is YES, in [0, 1]
 */
public record DecisionAnswer(String id, double yesProbability) {
}
