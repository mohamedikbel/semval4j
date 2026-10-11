package io.semval4j.core;

import io.semval4j.core.model.SemanticAnswer;
import io.semval4j.core.model.SemanticQuestion;
import io.semval4j.core.model.SemanticState;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelRecordsTest {

    @Test
    void stateIsAnImmutableCopyOfTheInput() {
        Map<String, Object> values = new HashMap<>();
        values.put("comment", "hello");

        SemanticState state = new SemanticState(values);
        values.put("injected", "later");

        assertThat(state.values()).containsOnly(Map.entry("comment", "hello"));
        assertThatThrownBy(() -> state.values().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void questionRejectsBlankFields() {
        assertThatThrownBy(() -> new SemanticQuestion("", "q"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SemanticQuestion("id", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void answerRejectsProbabilitiesOutsideZeroOne() {
        assertThatThrownBy(() -> new SemanticAnswer("id", 1.5, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SemanticAnswer("id", Double.NaN, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new SemanticAnswer("id", 0.0, null).probability()).isZero();
        assertThat(new SemanticAnswer("id", 1.0, null).probability()).isOne();
    }
}
