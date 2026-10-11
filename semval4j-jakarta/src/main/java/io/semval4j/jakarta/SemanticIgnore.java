package io.semval4j.jakarta;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excludes a property from the {@code SemanticState} sent to the Decision Model.
 *
 * <p><strong>Privacy:</strong> the whole object state is transmitted to the
 * configured provider over the network. Annotate any sensitive component
 * (passwords, tokens, personal identifiers, payment data...) so it never
 * leaves the process.
 *
 * <pre>{@code
 * public record Account(@NotBlank String email,
 *                       @SemanticIgnore String password) {}
 * }</pre>
 */
@Documented
@Target({ElementType.RECORD_COMPONENT, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SemanticIgnore {
}
