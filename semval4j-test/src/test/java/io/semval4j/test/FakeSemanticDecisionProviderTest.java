package io.semval4j.test;

import io.semval4j.core.model.SemanticDecisionResult;
import io.semval4j.core.model.SemanticQuestion;
import io.semval4j.core.model.SemanticState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakeSemanticDecisionProviderTest {

    private static final SemanticState STATE = new SemanticState(Map.of("comment", "nice"));

    @Test
    void returnsConfiguredProbabilities() {
        FakeSemanticDecisionProvider provider = FakeSemanticDecisionProvider.builder()
                .answer("rating-consistency", 0.04)
                .answer("product-relevance", 0.98)
                .build();

        SemanticDecisionResult result = provider.evaluate(STATE, List.of(
                new SemanticQuestion("rating-consistency", "q1"),
                new SemanticQuestion("product-relevance", "q2")));

        assertThat(result.answers()).extracting(a -> a.id() + "=" + a.probability())
                .containsExactly("rating-consistency=0.04", "product-relevance=0.98");
        assertThat(result.provider()).isEqualTo("fake");
    }

    @Test
    void recordsInvocationsForBatchingAssertions() {
        FakeSemanticDecisionProvider provider = FakeSemanticDecisionProvider.builder()
                .defaultProbability(1.0)
                .build();

        provider.evaluate(STATE, List.of(new SemanticQuestion("a", "q"), new SemanticQuestion("b", "q")));

        assertThat(provider.invocationCount()).isEqualTo(1);
        assertThat(provider.invocations()).singleElement().satisfies(invocation -> {
            assertThat(invocation.state()).isEqualTo(STATE);
            assertThat(invocation.questions()).extracting(SemanticQuestion::id)
                    .containsExactly("a", "b");
        });
    }

    @Test
    void unknownRuleWithoutDefaultFails() {
        FakeSemanticDecisionProvider provider = FakeSemanticDecisionProvider.builder().build();

        assertThatThrownBy(() -> provider.evaluate(STATE, List.of(new SemanticQuestion("unknown", "q"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void builderRejectsInvalidProbabilities() {
        assertThatThrownBy(() -> FakeSemanticDecisionProvider.builder().answer("a", 1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
