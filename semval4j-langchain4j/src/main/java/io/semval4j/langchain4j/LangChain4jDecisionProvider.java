package io.semval4j.langchain4j;

import io.semval4j.core.exception.SemValProviderException;
import io.semval4j.core.model.SemanticAnswer;
import io.semval4j.core.model.SemanticDecisionResult;
import io.semval4j.core.model.SemanticQuestion;
import io.semval4j.core.model.SemanticState;
import io.semval4j.core.provider.ProviderExceptions;
import io.semval4j.core.provider.SemanticDecisionProvider;

import java.util.List;
import java.util.Objects;

/**
 * {@link SemanticDecisionProvider} backed by a LangChain4j-style
 * {@link DecisionModel}.
 *
 * <p>Every call builds exactly one {@link DecisionRequest} containing the state
 * and all questions, calls {@code decisionModel.decide(...)} once, extracts the
 * YES probability per question and returns SemVal4j-owned records only.
 */
public final class LangChain4jDecisionProvider implements SemanticDecisionProvider {

    private final DecisionModel decisionModel;
    private final String model;

    /**
     * Creates a provider using the model's own default decision model.
     */
    public LangChain4jDecisionProvider(DecisionModel decisionModel) {
        this(decisionModel, null);
    }

    /**
     * Creates a provider pinned to a specific decision model identifier.
     */
    public LangChain4jDecisionProvider(DecisionModel decisionModel, String model) {
        this.decisionModel = Objects.requireNonNull(decisionModel, "decisionModel must not be null");
        this.model = model;
    }

    @Override
    public SemanticDecisionResult evaluate(SemanticState state, List<SemanticQuestion> questions) {
        DecisionRequest request = new DecisionRequest(
                model,
                state.values(),
                questions.stream()
                        .map(q -> new DecisionQuestion(q.id(), q.question()))
                        .toList());

        DecisionResponse response = ProviderExceptions.wrap(() -> decisionModel.decide(request));
        if (response == null) {
            throw new SemValProviderException("Decision model returned null response");
        }

        return new SemanticDecisionResult(
                response.answers().stream()
                        .map(a -> new SemanticAnswer(a.id(), a.yesProbability(), null))
                        .toList(),
                "langchain4j",
                model);
    }
}
