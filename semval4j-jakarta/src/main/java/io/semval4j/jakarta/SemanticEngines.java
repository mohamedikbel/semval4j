package io.semval4j.jakarta;

import io.semval4j.core.engine.SemanticValidationEngine;
import io.semval4j.core.exception.SemValConfigurationException;

import java.util.Objects;

/**
 * Integration point between the Jakarta {@code ConstraintValidator} and the
 * application-configured {@link SemanticValidationEngine}.
 *
 * <p>Constraint validators are instantiated by the Bean Validation provider,
 * not by the application, so the engine is published through this locator.
 * The Spring Boot starter registers its engine automatically; plain Jakarta
 * users call {@link #register(SemanticValidationEngine)} once at startup.
 */
public final class SemanticEngines {

    private static volatile SemanticValidationEngine engine;

    private SemanticEngines() {
    }

    /** Publishes the engine used by all {@link SemanticObjectValidator} instances. */
    public static void register(SemanticValidationEngine engine) {
        SemanticEngines.engine = Objects.requireNonNull(engine, "engine must not be null");
    }

    /** Removes the registered engine (mainly for tests). */
    public static void clear() {
        engine = null;
    }

    /**
     * @return the registered engine
     * @throws SemValConfigurationException when no engine was registered
     */
    public static SemanticValidationEngine get() {
        SemanticValidationEngine current = engine;
        if (current == null) {
            throw new SemValConfigurationException(
                    "No SemanticValidationEngine registered. Add the SemVal4j Spring Boot starter "
                            + "or call SemanticEngines.register(engine) at startup.");
        }
        return current;
    }
}
