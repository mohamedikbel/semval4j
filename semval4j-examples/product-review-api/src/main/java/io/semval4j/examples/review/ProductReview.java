package io.semval4j.examples.review;

import io.semval4j.jakarta.SemanticRule;
import io.semval4j.jakarta.SemanticValid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Structurally valid by Jakarta rules, semantically checked by SemVal4j:
 * a 1-star rating conflicts with strongly positive language.
 */
@SemanticValid({
        @SemanticRule(
                id = "rating-consistency",
                question = "Is the sentiment of the review consistent with the numeric rating?",
                property = "comment",
                passAt = 0.85,
                failBelow = 0.20
        ),
        @SemanticRule(
                id = "product-relevance",
                question = "Does the comment express an opinion about the product, its features or its usage, rather than being off-topic?",
                property = "comment",
                passAt = 0.85,
                failBelow = 0.20
        )
})
public record ProductReview(
        @NotBlank String product,
        @Min(1) @Max(5) int rating,
        @NotBlank @Size(max = 2000) String comment
) {
}
