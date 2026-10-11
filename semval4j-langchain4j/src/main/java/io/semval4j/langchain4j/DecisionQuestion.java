package io.semval4j.langchain4j;

/**
 * One yes/no decision question, as sent to a LangChain4j-style Decision Model.
 *
 * @param id       question identifier, correlated with {@link DecisionAnswer#id()}
 * @param question natural language yes/no question
 */
public record DecisionQuestion(String id, String question) {
}
