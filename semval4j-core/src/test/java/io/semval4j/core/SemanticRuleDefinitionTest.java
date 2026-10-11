package io.semval4j.core;

import io.semval4j.core.exception.SemValConfigurationException;
import io.semval4j.core.model.DecisionOutcome;
import io.semval4j.core.model.SemanticRuleDefinition;
import io.semval4j.core.policy.UncertainPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemanticRuleDefinitionTest {

    @Test
    void rejectsBlankId() {
        assertThatThrownBy(() -> SemanticRuleDefinition.of(" ", "q"))
                .isInstanceOf(SemValConfigurationException.class);
        assertThatThrownBy(() -> SemanticRuleDefinition.of(null, "q"))
                .isInstanceOf(SemValConfigurationException.class);
    }

    @Test
    void rejectsBlankQuestion() {
        assertThatThrownBy(() -> SemanticRuleDefinition.of("r1", ""))
                .isInstanceOf(SemValConfigurationException.class);
    }

    @Test
    void rejectsThresholdsOutsideZeroOne() {
        assertThatThrownBy(() -> new SemanticRuleDefinition("r1", "q", "", 1.01, 0.2, UncertainPolicy.FAIL))
                .isInstanceOf(SemValConfigurationException.class);
        assertThatThrownBy(() -> new SemanticRuleDefinition("r1", "q", "", 0.85, -0.01, UncertainPolicy.FAIL))
                .isInstanceOf(SemValConfigurationException.class);
    }

    @Test
    void rejectsFailBelowGreaterOrEqualToPassAt() {
        assertThatThrownBy(() -> new SemanticRuleDefinition("r1", "q", "", 0.5, 0.5, UncertainPolicy.FAIL))
                .isInstanceOf(SemValConfigurationException.class)
                .hasMessageContaining("failBelow");
        assertThatThrownBy(() -> new SemanticRuleDefinition("r1", "q", "", 0.4, 0.9, UncertainPolicy.FAIL))
                .isInstanceOf(SemValConfigurationException.class);
    }

    @Test
    void nullPolicyDefaultsToFailAndNullPropertyToEmpty() {
        SemanticRuleDefinition rule = new SemanticRuleDefinition("r1", "q", null, 0.85, 0.20, null);

        assertThat(rule.uncertain()).isEqualTo(UncertainPolicy.FAIL);
        assertThat(rule.property()).isEmpty();
    }

    @Test
    void classifiesBoundaryProbabilities() {
        SemanticRuleDefinition rule = new SemanticRuleDefinition("r1", "q", "", 0.85, 0.20, UncertainPolicy.FAIL);

        assertThat(rule.outcomeFor(0.85)).isEqualTo(DecisionOutcome.VALID);
        assertThat(rule.outcomeFor(1.0)).isEqualTo(DecisionOutcome.VALID);
        assertThat(rule.outcomeFor(0.20)).isEqualTo(DecisionOutcome.INVALID);
        assertThat(rule.outcomeFor(0.0)).isEqualTo(DecisionOutcome.INVALID);
        assertThat(rule.outcomeFor(0.5)).isEqualTo(DecisionOutcome.UNCERTAIN);
        assertThat(rule.outcomeFor(0.84)).isEqualTo(DecisionOutcome.UNCERTAIN);
        assertThat(rule.outcomeFor(0.21)).isEqualTo(DecisionOutcome.UNCERTAIN);
    }
}
