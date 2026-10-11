package io.semval4j.test;

import io.semval4j.core.model.SemanticAnswer;
import io.semval4j.core.model.SemanticDecisionResult;
import io.semval4j.core.model.SemanticQuestion;
import io.semval4j.core.model.SemanticState;
import io.semval4j.core.provider.SemanticDecisionProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic {@link SemanticDecisionProvider} for tests.
 *
 * <p>No network, no API key: probabilities are declared up front and every
 * invocation is recorded so tests can assert batching behaviour
 * (exactly one call per validated object).
 *
 * <pre>{@code
 * FakeSemanticDecisionProvider provider = FakeSemanticDecisionProvider.builder()
 *         .answer("rating-consistency", 0.04)
 *         .answer("product-relevance", 0.98)
 *         .build();
 * }</pre>
 */
public final class FakeSemanticDecisionProvider implements SemanticDecisionProvider {

    private final Map<String, Double> answers;
    private final Double defaultProbability;

    private final AtomicInteger invocationCount = new AtomicInteger();
    private final List<Invocation> invocations = new CopyOnWriteArrayList<>();

    private FakeSemanticDecisionProvider(Map<String, Double> answers, Double defaultProbability) {
        this.answers = Map.copyOf(answers);
        this.defaultProbability = defaultProbability;
    }

    /**
     * A recorded provider call.
     *
     * @param state     state passed to the provider
     * @param questions questions passed to the provider
     */
    public record Invocation(SemanticState state, List<SemanticQuestion> questions) {
        public Invocation {
            questions = List.copyOf(questions);
        }
    }

    @Override
    public SemanticDecisionResult evaluate(SemanticState state, List<SemanticQuestion> questions) {
        invocationCount.incrementAndGet();
        invocations.add(new Invocation(state, questions));

        List<SemanticAnswer> answers = new ArrayList<>(questions.size());
        for (SemanticQuestion question : questions) {
            Double probability = this.answers.get(question.id());
            if (probability == null) {
                if (defaultProbability == null) {
                    throw new IllegalStateException(
                            "No fake answer configured for rule '" + question.id()
                                    + "'. Use builder().answer(id, probability) or defaultProbability(p).");
                }
                probability = defaultProbability;
            }
            answers.add(new SemanticAnswer(question.id(), probability, null));
        }
        return new SemanticDecisionResult(answers, "fake", "fake-1");
    }

    /**
     * @return how many times {@link #evaluate} was called; batching means one call per object
     */
    public int invocationCount() {
        return invocationCount.get();
    }

    /**
     * @return every recorded invocation, in order
     */
    public List<Invocation> invocations() {
        return List.copyOf(invocations);
    }

    /** Resets invocation count and history. */
    public void reset() {
        invocationCount.set(0);
        invocations.clear();
    }

    /**
     * @return a new builder for deterministic answers
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for deterministic answers. */
    public static final class Builder {

        private final Map<String, Double> answers = new LinkedHashMap<>();
        private Double defaultProbability;

        private Builder() {
        }

        /** Configures the YES probability for one rule id. */
        public Builder answer(String id, double probability) {
            Objects.requireNonNull(id, "id must not be null");
            answers.put(id, checkProbability(probability));
            return this;
        }

        /** Fallback probability used for rules without an explicit answer. */
        public Builder defaultProbability(double probability) {
            this.defaultProbability = checkProbability(probability);
            return this;
        }

        /**
         * @return a ready-to-use fake provider with the configured answers
         */
        public FakeSemanticDecisionProvider build() {
            return new FakeSemanticDecisionProvider(answers, defaultProbability);
        }

        private static double checkProbability(double probability) {
            if (probability < 0.0 || probability > 1.0 || Double.isNaN(probability)) {
                throw new IllegalArgumentException("probability must be between 0 and 1, got " + probability);
            }
            return probability;
        }
    }
}
