package io.semval4j.jakarta;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level constraint declaring semantic rules on a Java type.
 *
 * <p>All rules are batched into a single Decision Model call and each failed
 * rule produces one standard {@code ConstraintViolation}. Keep using
 * {@code @Valid} exactly as with structural constraints.
 */
@Documented
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SemanticObjectValidator.class)
public @interface SemanticValid {

    /** The semantic rules to evaluate for the annotated type. */
    SemanticRule[] value();

    /** Prefix of the violation message; the rule id, outcome and probability are appended. */
    String message() default "Semantic validation failed";

    /** Validation groups this constraint belongs to (standard Bean Validation). */
    Class<?>[] groups() default {};

    /** Payload for clients of the constraint (standard Bean Validation). */
    Class<? extends Payload>[] payload() default {};
}
