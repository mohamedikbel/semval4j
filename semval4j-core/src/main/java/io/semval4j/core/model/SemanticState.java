package io.semval4j.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot of the object being validated, as a flat property map.
 *
 * <p>The state is serialized once and shared by every semantic question of the
 * object, so all rules are evaluated against exactly the same data.
 */
public record SemanticState(Map<String, Object> values) {

    public SemanticState {
        Objects.requireNonNull(values, "values must not be null");
        values = Map.copyOf(values);
    }

    /**
     * Creates a state from the given property map.
     *
     * @param values property name to property value, never {@code null}
     * @return a new immutable state
     */
    public static SemanticState of(Map<String, Object> values) {
        return new SemanticState(values);
    }
}
