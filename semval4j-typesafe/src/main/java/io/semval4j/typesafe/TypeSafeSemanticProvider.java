package io.semval4j.typesafe;

import io.semval4j.core.provider.SemanticDecisionProvider;
import io.semval4j.langchain4j.LangChain4jDecisionProvider;

import java.time.Duration;
import java.util.Objects;

/**
 * Convenience builder for the real Jev/TypeSafe-backed {@link SemanticDecisionProvider}.
 *
 * <p>By default it calls the official TypeSafe System One API directly
 * ({@code POST https://api.typesafe.ai/v1/systemone}) with a TypeSafe API key
 * from <a href="https://console.typesafe.ai/keys">console.typesafe.ai</a>.
 * To go through OpenRouter instead, override {@link Builder#baseUrl(String)} with
 * {@code "https://openrouter.ai"} and {@link Builder#decisionsPath(String)} with
 * {@code "/api/alpha/decisions"}, and use model {@code "typesafe/jev-1.13"}.
 *
 * <pre>{@code
 * SemanticDecisionProvider provider = TypeSafeSemanticProvider.builder()
 *         .apiKey(System.getenv("TYPESAFE_API_KEY"))
 *         .build();
 * }</pre>
 */
public final class TypeSafeSemanticProvider {

    /** Default endpoint: the official TypeSafe System One API. */
    public static final String DEFAULT_BASE_URL = "https://api.typesafe.ai";

    /** System One API path appended to the base URL. */
    public static final String DECISIONS_PATH = "/v1/systemone";

    /** Default decision model identifier. */
    public static final String DEFAULT_MODEL = "jev-latest";

    private TypeSafeSemanticProvider() {
    }

    /**
     * @return a new builder; {@code apiKey} is required, everything else has production defaults
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder for the TypeSafe provider. */
    public static final class Builder {

        private String apiKey;
        private String model = DEFAULT_MODEL;
        private String baseUrl = DEFAULT_BASE_URL;
        private String decisionsPath = DECISIONS_PATH;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration requestTimeout = Duration.ofSeconds(10);

        private Builder() {
        }

        /** TypeSafe API key. Required; keep it in configuration, never in code or annotations. */
        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /** Decision model identifier, defaults to {@value #DEFAULT_MODEL}. */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /** API base URL, defaults to {@value #DEFAULT_BASE_URL} (the direct TypeSafe endpoint). */
        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        /** API path, defaults to {@value #DECISIONS_PATH}. Override with {@code "/api/alpha/decisions"} for OpenRouter. */
        public Builder decisionsPath(String decisionsPath) {
            this.decisionsPath = decisionsPath;
            return this;
        }

        /** HTTP connect timeout, default 5 seconds. */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout);
            return this;
        }

        /** HTTP request timeout, default 10 seconds. */
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout);
            return this;
        }

        /**
         * @return a ready-to-use {@link SemanticDecisionProvider}
         * @throws io.semval4j.core.exception.SemValProviderException when the API key is missing
         */
        public SemanticDecisionProvider build() {
            return new LangChain4jDecisionProvider(
                    new TypeSafeJevDecisionModel(baseUrl, apiKey, model, connectTimeout, requestTimeout,
                            decisionsPath),
                    model);
        }
    }
}
