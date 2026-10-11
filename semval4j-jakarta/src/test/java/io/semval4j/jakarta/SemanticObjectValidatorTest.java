package io.semval4j.jakarta;

import io.semval4j.core.engine.SemanticValidationEngine;
import io.semval4j.core.policy.UncertainPolicy;
import io.semval4j.test.FakeSemanticDecisionProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticObjectValidatorTest {

    @SemanticValid({
            @SemanticRule(
                    id = "rating-consistency",
                    question = "Is the sentiment of the review consistent with the numeric rating?",
                    property = "comment",
                    passAt = 0.85,
                    failBelow = 0.20),
            @SemanticRule(
                    id = "product-relevance",
                    question = "Is the review actually about the specified product or its usage?",
                    property = "comment",
                    passAt = 0.85,
                    failBelow = 0.20)
    })
    record ProductReview(@NotBlank String product, @Min(1) int rating, @NotBlank String comment) {
    }

    @SemanticValid({
            @SemanticRule(id = "object-level", question = "Is the object coherent?")
    })
    record ObjectLevel(String name) {
    }

    @SemanticValid({
            @SemanticRule(id = "uncertain-passes", question = "q", uncertain = UncertainPolicy.PASS)
    })
    record PermissiveReview(String comment) {
    }

    @SemanticValid({
            @SemanticRule(id = "any", question = "q", passAt = 0.5, failBelow = 0.1)
    })
    record Account(String email, @SemanticIgnore String password) {
    }

    private FakeSemanticDecisionProvider provider;
    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
        SemanticEngines.clear();
    }

    private void useProvider(FakeSemanticDecisionProvider provider) {
        this.provider = provider;
        SemanticEngines.register(new SemanticValidationEngine(provider));
    }

    @Test
    void invalidRuleProducesOneViolationAttachedToConfiguredProperty() {
        useProvider(FakeSemanticDecisionProvider.builder()
                .answer("rating-consistency", 0.04)
                .answer("product-relevance", 0.98)
                .build());

        Set<ConstraintViolation<ProductReview>> violations = validator.validate(
                new ProductReview("Samsung Galaxy S26", 1, "Excellent phone, fast, great screen, I recommend it!"));

        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.getPropertyPath()).hasToString("comment");
            assertThat(violation.getMessage())
                    .contains("rating-consistency")
                    .contains("INVALID");
        });
    }

    @Test
    void twoRulesCauseExactlyOneProviderInvocation() {
        useProvider(FakeSemanticDecisionProvider.builder()
                .answer("rating-consistency", 0.99)
                .answer("product-relevance", 0.97)
                .build());

        Set<ConstraintViolation<ProductReview>> violations = validator.validate(
                new ProductReview("Samsung Galaxy S26", 5, "Excellent phone"));

        assertThat(violations).isEmpty();
        assertThat(provider.invocationCount()).isEqualTo(1);
        assertThat(provider.invocations().get(0).questions()).hasSize(2);
        assertThat(provider.invocations().get(0).state().values())
                .containsEntry("product", "Samsung Galaxy S26")
                .containsEntry("rating", 5);
    }

    @Test
    void nullObjectIsValid() {
        // Bean Validation forbids Validator.validate(null); the "null is valid"
        // contract applies to the ConstraintValidator itself, matching @NotNull conventions.
        SemanticObjectValidator objectValidator = new SemanticObjectValidator();
        objectValidator.initialize(ProductReview.class.getAnnotation(SemanticValid.class));

        assertThat(objectValidator.isValid(null, null)).isTrue();
    }

    @Test
    void violationWithoutPropertyIsAttachedAtObjectLevel() {
        useProvider(FakeSemanticDecisionProvider.builder()
                .answer("object-level", 0.05)
                .build());

        Set<ConstraintViolation<ObjectLevel>> violations = validator.validate(new ObjectLevel("x"));

        assertThat(violations).singleElement().satisfies(violation ->
                assertThat(violation.getPropertyPath()).hasToString(""));
    }

    @Test
    void uncertainProbabilityFollowsTheConfiguredPolicy() {
        useProvider(FakeSemanticDecisionProvider.builder()
                .answer("uncertain-passes", 0.62)
                .build());

        assertThat(validator.validate(new PermissiveReview("maybe"))).isEmpty();

        useProvider(FakeSemanticDecisionProvider.builder()
                .answer("rating-consistency", 0.62)
                .answer("product-relevance", 0.99)
                .build());

        assertThat(validator.validate(new ProductReview("p", 3, "ok"))).hasSize(1);
    }

    @Test
    void multipleFailedRulesProduceMultipleViolations() {
        useProvider(FakeSemanticDecisionProvider.builder()
                .answer("rating-consistency", 0.04)
                .answer("product-relevance", 0.02)
                .build());

        Set<ConstraintViolation<ProductReview>> violations = validator.validate(
                new ProductReview("Samsung Galaxy S26", 1, "I love it"));

        assertThat(violations).hasSize(2);
        assertThat(violations).allSatisfy(v -> assertThat(v.getPropertyPath()).hasToString("comment"));
    }

    @Test
    void semanticIgnoreExcludesSensitiveFieldsFromTheStateSentToTheProvider() {
        useProvider(FakeSemanticDecisionProvider.builder()
                .answer("any", 0.99)
                .build());

        assertThat(validator.validate(new Account("user@example.com", "s3cr3t!"))).isEmpty();

        assertThat(provider.invocations()).singleElement().satisfies(invocation -> {
            assertThat(invocation.state().values())
                    .containsEntry("email", "user@example.com")
                    .doesNotContainKey("password");
            assertThat(invocation.state().values().toString()).doesNotContain("s3cr3t!");
        });
    }
}
