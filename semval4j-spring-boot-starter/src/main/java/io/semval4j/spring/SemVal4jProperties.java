package io.semval4j.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration properties for SemVal4j.
 *
 * <pre>
 * semval4j:
 *   enabled: true
 *   provider: typesafe
 *   api-key: ${TYPESAFE_API_KEY}
 *   model: jev-1.13.0
 * </pre>
 *
 * @param enabled       whether semantic validation auto-configuration is active
 * @param provider      provider name; {@code "typesafe"} (Jev) ships with V0
 * @param apiKey        provider API key; a configuration secret, never logged or serialized
 * @param model         decision model identifier (e.g. {@code "jev-latest"})
 * @param baseUrl       provider endpoint override (mainly for tests and gateways)
 * @param decisionsPath provider API path override (e.g. {@code "/api/alpha/decisions"} for OpenRouter)
 */
@ConfigurationProperties("semval4j")
public record SemVal4jProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("typesafe") String provider,
        String apiKey,
        @DefaultValue("jev-latest") String model,
        String baseUrl,
        String decisionsPath) {

    /**
     * Redacted rendering: the record-generated {@code toString()} would print
     * the live API key, so it is masked here.
     */
    @Override
    public String toString() {
        return "SemVal4jProperties[enabled=" + enabled
                + ", provider=" + provider
                + ", apiKey=" + (apiKey == null || apiKey.isBlank() ? "<unset>" : "***")
                + ", model=" + model
                + ", baseUrl=" + baseUrl
                + ", decisionsPath=" + decisionsPath + "]";
    }
}
