package io.semval4j.spring;

import io.semval4j.core.engine.SemanticValidationEngine;
import io.semval4j.core.provider.SemanticDecisionProvider;
import io.semval4j.test.FakeSemanticDecisionProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemVal4jAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SemVal4jAutoConfiguration.class));

    @Test
    void disabledByDefault() {
        contextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(SemanticDecisionProvider.class);
            assertThat(context).doesNotHaveBean(SemanticValidationEngine.class);
        });
    }

    @Test
    void createsProviderAndEngineWhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "semval4j.enabled=true",
                        "semval4j.provider=typesafe",
                        "semval4j.api-key=test-key",
                        "semval4j.model=jev-1.13.0")
                .run(context -> {
                    assertThat(context).hasSingleBean(SemanticDecisionProvider.class);
                    assertThat(context).hasSingleBean(SemanticValidationEngine.class);
                });
    }

    @Test
    void failsFastWhenApiKeyIsMissing() {
        contextRunner
                .withPropertyValues(
                        "semval4j.enabled=true",
                        "semval4j.provider=typesafe")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("semval4j.api-key");
                });
    }

    @Test
    void failsFastForUnknownProvider() {
        contextRunner
                .withPropertyValues(
                        "semval4j.enabled=true",
                        "semval4j.provider=unknown",
                        "semval4j.api-key=test-key")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("unknown");
                });
    }

    @Test
    void backsOffWhenUserProvidesCustomProvider() {
        FakeSemanticDecisionProvider fake = FakeSemanticDecisionProvider.builder()
                .defaultProbability(1.0)
                .build();

        contextRunner
                .withPropertyValues("semval4j.enabled=true")
                .withBean(SemanticDecisionProvider.class, () -> fake)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).getBean(SemanticDecisionProvider.class).isSameAs(fake);
                    assertThat(context).hasSingleBean(SemanticValidationEngine.class);
                });
    }

    @Test
    void propertiesToStringNeverExposesTheApiKey() {
        SemVal4jProperties properties =
                new SemVal4jProperties(true, "typesafe", "sk-live-secret", "jev-latest", null, null);

        assertThat(properties.toString())
                .contains("apiKey=***")
                .doesNotContain("sk-live-secret");

        assertThat(new SemVal4jProperties(true, "typesafe", null, "jev-latest", null, null).toString())
                .contains("apiKey=<unset>");
    }

    @Test
    void engineRegistrationIsClearedWhenContextCloses() {
        contextRunner
                .withPropertyValues(
                        "semval4j.enabled=true",
                        "semval4j.provider=typesafe",
                        "semval4j.api-key=test-key")
                .run(context -> {
                    assertThat(io.semval4j.jakarta.SemanticEngines.get()).isNotNull();
                    context.close();
                    assertThatThrownBy(io.semval4j.jakarta.SemanticEngines::get)
                            .hasMessageContaining("No SemanticValidationEngine registered");
                });
    }

    @Test
    void backsOffWhenUserProvidesCustomEngine() {
        SemanticValidationEngine custom = new SemanticValidationEngine(
                FakeSemanticDecisionProvider.builder().defaultProbability(1.0).build());

        contextRunner
                .withPropertyValues("semval4j.enabled=true")
                .withBean(SemanticDecisionProvider.class,
                        () -> FakeSemanticDecisionProvider.builder().defaultProbability(1.0).build())
                .withBean(SemanticValidationEngine.class, () -> custom)
                .run(context -> assertThat(context)
                        .getBean(SemanticValidationEngine.class).isSameAs(custom));
    }
}
