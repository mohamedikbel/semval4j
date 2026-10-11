package io.semval4j.core.provider;

import io.semval4j.core.model.SemanticDecisionResult;
import io.semval4j.core.model.SemanticQuestion;
import io.semval4j.core.model.SemanticState;

import java.util.List;

/**
 * SPI implemented by Decision Model providers (Jev/TypeSafe, future Clef, custom...).
 *
 * <p>All questions of one object are evaluated in a single call so that
 * batching is a property of the contract, not of an implementation detail.
 *
 * <p>Implementations must throw only SemVal4j-owned exceptions
 * ({@code SemValProviderException} and friends); SDK exceptions must be wrapped.
 */
public interface SemanticDecisionProvider {

    /**
     * Evaluates all questions against the given state in one batched call.
     *
     * @param state     serialized snapshot of the validated object
     * @param questions the yes/no questions to evaluate, never empty when called
     * @return provider-neutral answers, one per question id
     */
    SemanticDecisionResult evaluate(SemanticState state, List<SemanticQuestion> questions);
}
