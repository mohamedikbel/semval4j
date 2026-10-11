package io.semval4j.langchain4j;

import io.semval4j.core.exception.SemValProviderException;
import io.semval4j.core.model.SemanticDecisionResult;
import io.semval4j.core.model.SemanticQuestion;
import io.semval4j.core.model.SemanticState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LangChain4jDecisionProviderTest {

    private static final SemanticState STATE = new SemanticState(Map.of("comment", "great"));

    @Test
    void batchesAllQuestionsIntoOneDecisionRequest() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<DecisionRequest> captured = new AtomicReference<>();
        DecisionModel model = request -> {
            calls.incrementAndGet();
            captured.set(request);
            return new DecisionResponse(request.questions().stream()
                    .map(q -> new DecisionAnswer(q.id(), 0.9))
                    .toList());
        };
        LangChain4jDecisionProvider provider = new LangChain4jDecisionProvider(model, "jev-1.13.0");

        SemanticDecisionResult result = provider.evaluate(STATE, List.of(
                new SemanticQuestion("rating-consistency", "Is sentiment consistent?"),
                new SemanticQuestion("product-relevance", "Is it about the product?")));

        assertThat(calls).hasValue(1);
        assertThat(captured.get().model()).isEqualTo("jev-1.13.0");
        assertThat(captured.get().state()).containsEntry("comment", "great");
        assertThat(captured.get().questions()).extracting(DecisionQuestion::id)
                .containsExactly("rating-consistency", "product-relevance");

        assertThat(result.answers()).extracting(a -> a.id() + "=" + a.probability())
                .containsExactly("rating-consistency=0.9", "product-relevance=0.9");
        assertThat(result.provider()).isEqualTo("langchain4j");
        assertThat(result.model()).isEqualTo("jev-1.13.0");
    }

    @Test
    void wrapsModelExceptionsInProviderNeutralType() {
        DecisionModel model = request -> {
            throw new RuntimeException("connection reset");
        };

        assertThatThrownBy(() -> new LangChain4jDecisionProvider(model)
                .evaluate(STATE, List.of(new SemanticQuestion("a", "q"))))
                .isInstanceOf(SemValProviderException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void nullResponseFailsWithProviderException() {
        DecisionModel model = request -> null;

        assertThatThrownBy(() -> new LangChain4jDecisionProvider(model)
                .evaluate(STATE, List.of(new SemanticQuestion("a", "q"))))
                .isInstanceOf(SemValProviderException.class);
    }
}
