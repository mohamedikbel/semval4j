package io.semval4j.jakarta;

import io.semval4j.core.model.SemanticRuleDefinition;
import io.semval4j.core.policy.UncertainPolicy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * One semantic rule inside a {@link SemanticValid} constraint.
 *
 * <p>The {@link #question()} is evaluated by the configured Decision Model
 * against the whole object state; the YES probability is classified with
 * {@link #passAt()} / {@link #failBelow()}.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface SemanticRule {

    /** Unique identifier of the rule within the annotated type. */
    String id();

    /** Yes/no question evaluated by the Decision Model. */
    String question();

    /** Property the violation is attached to; empty means object level. */
    String property() default "";

    /** Probability at or above which the rule is VALID. */
    double passAt() default SemanticRuleDefinition.DEFAULT_PASS_AT;

    /** Probability at or below which the rule is INVALID. */
    double failBelow() default SemanticRuleDefinition.DEFAULT_FAIL_BELOW;

    /** What to do when the probability falls between the thresholds. */
    UncertainPolicy uncertain() default UncertainPolicy.FAIL;
}
