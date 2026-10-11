package io.semval4j.jakarta;

import io.semval4j.core.engine.SemanticValidationEngine;
import io.semval4j.core.model.SemanticRuleDefinition;
import io.semval4j.core.model.SemanticRuleResult;
import io.semval4j.core.model.SemanticState;
import io.semval4j.core.model.SemanticValidationResult;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.List;

/**
 * Bridges Jakarta Bean Validation and the SemVal4j engine.
 *
 * <ul>
 *   <li>Rules are read once during {@link #initialize(SemanticValid)}.</li>
 *   <li>{@code null} objects are valid (use {@code @NotNull} for nullability).</li>
 *   <li>The engine is called once per validated object, never once per rule.</li>
 *   <li>Each failed rule yields one violation, attached to {@code rule.property()}
 *   when set, otherwise at object level.</li>
 * </ul>
 */
public class SemanticObjectValidator implements ConstraintValidator<SemanticValid, Object> {

    private List<SemanticRuleDefinition> rules = List.of();
    private String message;

    @Override
    public void initialize(SemanticValid constraintAnnotation) {
        this.message = constraintAnnotation.message();
        this.rules = Arrays.stream(constraintAnnotation.value())
                .map(rule -> new SemanticRuleDefinition(
                        rule.id(), rule.question(), rule.property(),
                        rule.passAt(), rule.failBelow(), rule.uncertain()))
                .toList();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null || rules.isEmpty()) {
            return true;
        }

        SemanticValidationEngine engine = SemanticEngines.get();
        SemanticState state = SemanticStateMapper.toSemanticState(value);
        SemanticValidationResult result = engine.validate(state, rules);

        if (result.isValid()) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        for (SemanticRuleResult failure : result.failures()) {
            String detail = message + " [rule=" + failure.rule().id()
                    + ", outcome=" + failure.outcome()
                    + ", probability=" + failure.probability() + "]";
            ConstraintValidatorContext.ConstraintViolationBuilder builder =
                    context.buildConstraintViolationWithTemplate(detail);
            if (!failure.rule().property().isEmpty()) {
                builder.addPropertyNode(failure.rule().property()).addConstraintViolation();
            } else {
                builder.addConstraintViolation();
            }
        }
        return false;
    }
}
