package io.semval4j.core;

import io.semval4j.core.engine.SemanticValidationEngine;
import io.semval4j.core.exception.SemValConfigurationException;
import io.semval4j.core.exception.SemValProviderException;
import io.semval4j.core.exception.SemValUncertainException;
import io.semval4j.core.model.DecisionOutcome;
import io.semval4j.core.model.SemanticAnswer;
import io.semval4j.core.model.SemanticDecisionResult;
import io.semval4j.core.model.SemanticQuestion;
import io.semval4j.core.model.SemanticRuleDefinition;
import io.semval4j.core.model.SemanticState;
import io.semval4j.core.model.SemanticValidationResult;
import io.semval4j.core.policy.UncertainPolicy;
import io.semval4j.core.provider.SemanticDecisionProvider;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemanticValidationEngineTest {

    private static final SemanticState STATE = new SemanticState(Map.of("comment", "great product"));

    private static SemanticRuleDefinition rule(String id) {
        return new SemanticRuleDefinition(id, "Is it consistent?", "comment", 0.85, 0.20, UncertainPolicy.FAIL);
    }

    private static SemanticRuleDefinition rule(String id, UncertainPolicy policy) {
        return new SemanticRuleDefinition(id, "Is it consistent?", "comment", 0.85, 0.20, policy);
    }

    private static SemanticDecisionProvider providerReturning(double probability) {
        return (state, questions) -> new SemanticDecisionResult(
                questions.stream()
                        .map(q -> new SemanticAnswer(q.id(), probability, null))
                        .toList(),
                "fake", "fake-1");
    }

    @Test
    void probabilityAtOrAbovePassAtIsValid() {
        SemanticValidationEngine engine = new SemanticValidationEngine(providerReturning(0.85));

        SemanticValidationResult result = engine.validate(STATE, List.of(rule("r1")));

        assertThat(result.isValid()).isTrue();
        assertThat(result.results()).singleElement().satisfies(r -> {
            assertThat(r.outcome()).isEqualTo(DecisionOutcome.VALID);
            assertThat(r.passed()).isTrue();
            assertThat(r.probability()).isEqualTo(0.85);
        });
    }

    @Test
    void probabilityAtOrBelowFailBelowIsInvalid() {
        SemanticValidationEngine engine = new SemanticValidationEngine(providerReturning(0.20));

        SemanticValidationResult result = engine.validate(STATE, List.of(rule("r1")));

        assertThat(result.isValid()).isFalse();
        assertThat(result.failures()).singleElement().satisfies(r -> {
            assertThat(r.outcome()).isEqualTo(DecisionOutcome.INVALID);
            assertThat(r.passed()).isFalse();
        });
    }

    @Test
    void probabilityBetweenThresholdsIsUncertainAndFailsByDefault() {
        SemanticValidationEngine engine = new SemanticValidationEngine(providerReturning(0.62));

        SemanticValidationResult result = engine.validate(STATE, List.of(rule("r1")));

        assertThat(result.isValid()).isFalse();
        assertThat(result.results().get(0).outcome()).isEqualTo(DecisionOutcome.UNCERTAIN);
        assertThat(result.results().get(0).passed()).isFalse();
    }

    @Test
    void uncertainPolicyPassTurnsUncertaintyIntoSuccess() {
        SemanticValidationEngine engine = new SemanticValidationEngine(providerReturning(0.62));

        SemanticValidationResult result = engine.validate(STATE, List.of(rule("r1", UncertainPolicy.PASS)));

        assertThat(result.isValid()).isTrue();
        assertThat(result.results().get(0).outcome()).isEqualTo(DecisionOutcome.UNCERTAIN);
        assertThat(result.results().get(0).passed()).isTrue();
    }

    @Test
    void uncertainPolicyErrorThrows() {
        SemanticValidationEngine engine = new SemanticValidationEngine(providerReturning(0.62));

        assertThatThrownBy(() -> engine.validate(STATE, List.of(rule("r1", UncertainPolicy.ERROR))))
                .isInstanceOf(SemValUncertainException.class)
                .satisfies(e -> {
                    SemValUncertainException ue = (SemValUncertainException) e;
                    assertThat(ue.ruleId()).isEqualTo("r1");
                    assertThat(ue.probability()).isEqualTo(0.62);
                });
    }

    @Test
    void allRulesOfOneObjectAreEvaluatedInASingleProviderCall() {
        AtomicInteger calls = new AtomicInteger();
        List<List<SemanticQuestion>> receivedQuestions = new ArrayList<>();
        SemanticDecisionProvider provider = (state, questions) -> {
            calls.incrementAndGet();
            receivedQuestions.add(questions);
            return new SemanticDecisionResult(
                    questions.stream().map(q -> new SemanticAnswer(q.id(), 0.99, null)).toList(),
                    "fake", "fake-1");
        };
        SemanticValidationEngine engine = new SemanticValidationEngine(provider);

        SemanticValidationResult result = engine.validate(STATE, List.of(rule("a"), rule("b"), rule("c")));

        assertThat(calls).hasValue(1);
        assertThat(receivedQuestions.get(0)).extracting(SemanticQuestion::id)
                .containsExactly("a", "b", "c");
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void emptyRuleListSkipsProviderCall() {
        AtomicInteger calls = new AtomicInteger();
        SemanticValidationEngine engine = new SemanticValidationEngine((state, questions) -> {
            calls.incrementAndGet();
            return new SemanticDecisionResult(List.of(), "fake", "fake-1");
        });

        SemanticValidationResult result = engine.validate(STATE, List.of());

        assertThat(calls).hasValue(0);
        assertThat(result.isValid()).isTrue();
        assertThat(result.results()).isEmpty();
    }

    @Test
    void duplicateRuleIdsAreRejected() {
        SemanticValidationEngine engine = new SemanticValidationEngine(providerReturning(1.0));

        assertThatThrownBy(() -> engine.validate(STATE, List.of(rule("dup"), rule("dup"))))
                .isInstanceOf(SemValConfigurationException.class)
                .hasMessageContaining("dup");
    }

    @Test
    void missingAnswerForRuleFailsWithProviderException() {
        SemanticDecisionProvider provider = (state, questions) -> new SemanticDecisionResult(
                List.of(new SemanticAnswer("other", 0.99, null)), "fake", "fake-1");
        SemanticValidationEngine engine = new SemanticValidationEngine(provider);

        assertThatThrownBy(() -> engine.validate(STATE, List.of(rule("r1"))))
                .isInstanceOf(SemValProviderException.class)
                .hasMessageContaining("r1");
    }

    @Test
    void duplicateAnswerIdsFailWithProviderException() {
        SemanticDecisionProvider provider = (state, questions) -> new SemanticDecisionResult(
                List.of(new SemanticAnswer("r1", 0.99, null), new SemanticAnswer("r1", 0.01, null)),
                "fake", "fake-1");
        SemanticValidationEngine engine = new SemanticValidationEngine(provider);

        assertThatThrownBy(() -> engine.validate(STATE, List.of(rule("r1"))))
                .isInstanceOf(SemValProviderException.class)
                .hasMessageContaining("r1");
    }

    @Test
    void providerExceptionsAreWrappedInProviderNeutralType() {
        SemanticDecisionProvider provider = (state, questions) -> {
            throw new IllegalStateException("sdk exploded");
        };
        SemanticValidationEngine engine = new SemanticValidationEngine(provider);

        assertThatThrownBy(() -> engine.validate(STATE, List.of(rule("r1"))))
                .isInstanceOf(SemValProviderException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void providerReturningNullFailsWithProviderException() {
        SemanticValidationEngine engine = new SemanticValidationEngine((state, questions) -> null);

        assertThatThrownBy(() -> engine.validate(STATE, List.of(rule("r1"))))
                .isInstanceOf(SemValProviderException.class);
    }

    @Test
    void engineRejectsNullProvider() {
        assertThatThrownBy(() -> new SemanticValidationEngine(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void providerSuppliedOutcomeIsHonoredOverThresholdClassification() {
        SemanticDecisionProvider provider = (state, questions) -> new SemanticDecisionResult(
                List.of(new SemanticAnswer("r1", 0.99, DecisionOutcome.INVALID)), "fake", "fake-1");
        SemanticValidationEngine engine = new SemanticValidationEngine(provider);

        SemanticValidationResult result = engine.validate(STATE, List.of(rule("r1")));

        assertThat(result.isValid()).isFalse();
        assertThat(result.results().get(0).outcome()).isEqualTo(DecisionOutcome.INVALID);
    }

    @Test
    void absentProviderOutcomeFallsBackToThresholdClassification() {
        SemanticDecisionProvider provider = (state, questions) -> new SemanticDecisionResult(
                List.of(new SemanticAnswer("r1", 0.99, null)), "fake", "fake-1");
        SemanticValidationEngine engine = new SemanticValidationEngine(provider);

        assertThat(engine.validate(STATE, List.of(rule("r1"))).isValid()).isTrue();
    }
}
