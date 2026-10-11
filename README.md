# SemVal4j

**Java validates structure. SemVal4j validates meaning.**

SemVal4j extends Jakarta Bean Validation with semantic, probabilistic rules powered by Decision Models.

Traditional validation:

```java
@NotNull, @Email, @Size, @Min...
```

Semantic validation:

```java
@SemanticRule(id = "rating-consistency",
              question = "Is the review sentiment consistent with the numeric rating?")
```

Use standard `@Valid`. SemVal4j batches semantic rules, asks the configured Decision Model **once**,
evaluates probabilities and returns ordinary `ConstraintViolation` objects.

## Why Decision Models?

Semantic validation is usually a bounded decision, not a text-generation task. Decision Models return
finite choices and probabilities, which maps naturally to validation rules. SemVal4j models an explicit
uncertainty band instead of pretending AI is deterministic:

```
0.00 -------- failBelow -------- passAt -------- 1.00
      INVALID       UNCERTAIN        VALID
```

## Quick start (Spring Boot)

```xml
<dependency>
  <groupId>io.github.mohamedikbel</groupId>
  <artifactId>semval4j-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

```yaml
semval4j:
  enabled: true
  provider: typesafe
  api-key: ${TYPESAFE_API_KEY}   # get one at https://console.typesafe.ai/keys
  model: jev-latest
```

Jev is called through the official TypeSafe System One API (`POST https://api.typesafe.ai/v1/systemone`).
Every semantic rule becomes a `noul` question, batched in a single request per validated object.
Prefer OpenRouter? Set `base-url: https://openrouter.ai`, `decisions-path: /api/alpha/decisions`
and `model: typesafe/jev-1.13`.

```java
@SemanticValid({
    @SemanticRule(
        id = "rating-consistency",
        question = "Is the sentiment of the review consistent with the numeric rating?",
        property = "comment",
        passAt = 0.85,
        failBelow = 0.20
    )
})
public record ProductReview(
    @NotBlank String product,
    @Min(1) @Max(5) int rating,
    @NotBlank String comment
) {}
```

```java
@PostMapping("/reviews")
public ResponseEntity<Void> create(@Valid @RequestBody ProductReview review) {
    reviewService.save(review);
    return ResponseEntity.noContent().build();
}
```

## Modules

| Module | Artifact | Purpose |
|---|---|---|
| Core | `io.github.mohamedikbel:semval4j-core` | Model, engine, thresholds, provider SPI. Zero framework deps. |
| Jakarta | `io.github.mohamedikbel:semval4j-jakarta` | `@SemanticValid` / `@SemanticRule`, `ConstraintValidator`. |
| LangChain4j adapter | `io.github.mohamedikbel:semval4j-langchain4j` | `DecisionModel` port and batching adapter. |
| TypeSafe/Jev | `io.github.mohamedikbel:semval4j-typesafe` | Convenience provider for Jev-compatible Decision Models over HTTP. |
| Spring Boot starter | `io.github.mohamedikbel:semval4j-spring-boot-starter` | Auto-configuration and properties. |
| Test support | `io.github.mohamedikbel:semval4j-test` | Deterministic fake provider for offline tests. |

## Testing without a network

```java
SemanticDecisionProvider provider = FakeSemanticDecisionProvider.builder()
        .answer("rating-consistency", 0.04)
        .answer("product-relevance", 0.98)
        .build();
```

## Design rules

- `semval4j-core` knows nothing about Spring, Jakarta Validation, LangChain4j or Jev.
- All semantic rules of one object are evaluated in a **single** provider call.
- Outcomes are `VALID`, `UNCERTAIN` and `INVALID`; the uncertainty band follows `UncertainPolicy`
  (`FAIL` by default, `PASS`, or `ERROR`).
- Provider failures are never silently swallowed; they surface as `SemValProviderException`.
- API keys are configuration secrets: never in annotations, never in serialized results
  (`SemVal4jProperties.toString()` redacts the key).

## Privacy: what leaves your process

The **entire object state** is sent to the configured Decision Model provider over the network.
Annotate sensitive fields with `@SemanticIgnore` to exclude them from the payload:

```java
public record Account(@NotBlank String email,
                      @SemanticIgnore String password,
                      @SemanticIgnore String socialSecurityNumber) {}
```

Semantic state is never logged by the library.

## License

Apache License 2.0, see [LICENSE](LICENSE).
