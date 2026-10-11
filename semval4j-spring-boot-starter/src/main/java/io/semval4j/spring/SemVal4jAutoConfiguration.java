package io.semval4j.spring;

import io.semval4j.core.engine.SemanticValidationEngine;
import io.semval4j.core.exception.SemValConfigurationException;
import io.semval4j.core.provider.SemanticDecisionProvider;
import io.semval4j.jakarta.SemanticEngines;
import io.semval4j.typesafe.TypeSafeSemanticProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;

/**
 * Wires SemVal4j into a Spring Boot application.
 *
 * <ul>
 *   <li>Creates a default {@link SemanticDecisionProvider} unless the application
 *   provides its own bean (back-off).</li>
 *   <li>Creates the {@link SemanticValidationEngine} and publishes it so the
 *   Jakarta {@code ConstraintValidator} picks it up under {@code @Valid}.</li>
 *   <li>Fails fast at startup when the provider is enabled but required
 *   credentials are missing.</li>
 * </ul>
 */
@AutoConfiguration
@EnableConfigurationProperties(SemVal4jProperties.class)
@ConditionalOnProperty(prefix = "semval4j", name = "enabled", havingValue = "true")
public class SemVal4jAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SemanticDecisionProvider semanticDecisionProvider(SemVal4jProperties properties) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new SemValConfigurationException(
                    "semval4j.api-key is required when semval4j.provider=" + properties.provider() + ". "
                            + "Set it via configuration (e.g. semval4j.api-key=${PROVIDER_API_KEY}).");
        }
        return switch (properties.provider()) {
            case "typesafe" -> {
                TypeSafeSemanticProvider.Builder builder = TypeSafeSemanticProvider.builder()
                        .apiKey(properties.apiKey())
                        .model(properties.model());
                if (properties.baseUrl() != null && !properties.baseUrl().isBlank()) {
                    builder.baseUrl(properties.baseUrl());
                }
                if (properties.decisionsPath() != null && !properties.decisionsPath().isBlank()) {
                    builder.decisionsPath(properties.decisionsPath());
                }
                yield builder.build();
            }
            default -> throw new SemValConfigurationException(
                    "Unknown semval4j.provider: '" + properties.provider()
                            + "'. Supported values: [typesafe]. "
                            + "Provide your own SemanticDecisionProvider bean for custom providers.");
        };
    }

    @Bean
    @ConditionalOnMissingBean
    SemanticValidationEngine semanticValidationEngine(SemanticDecisionProvider provider) {
        return new SemanticValidationEngine(provider);
    }

    @Bean
    SemanticEngineRegistrar semanticEngineRegistrar(SemanticValidationEngine engine) {
        return new SemanticEngineRegistrar(engine);
    }

    /**
     * Publishes the engine to the Jakarta integration point so
     * {@code SemanticObjectValidator} can resolve it without being a Spring bean.
     * Clears the registry when the context closes to avoid leaking a stale
     * engine across contexts (test suites, devtools restarts).
     */
    static final class SemanticEngineRegistrar implements DisposableBean {

        SemanticEngineRegistrar(SemanticValidationEngine engine) {
            SemanticEngines.register(engine);
        }

        @Override
        public void destroy() {
            SemanticEngines.clear();
        }
    }
}
