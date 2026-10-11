package io.semval4j.typesafe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.semval4j.core.exception.SemValProviderException;
import io.semval4j.langchain4j.DecisionAnswer;
import io.semval4j.langchain4j.DecisionModel;
import io.semval4j.langchain4j.DecisionRequest;
import io.semval4j.langchain4j.DecisionResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * HTTP binding of the real Jev Decision Model (TypeSafe AI) through the
 * System One API. The default endpoint is the official TypeSafe surface
 * ({@code https://api.typesafe.ai/v1/systemone}); OpenRouter
 * ({@code /api/alpha/decisions}) can be used by overriding base URL and path.
 *
 * <p>Wire format (one request per validated object, all questions batched,
 * every question is a {@code noul} primitive - probability that a statement
 * is true):
 *
 * <pre>
 * POST {baseUrl}{/v1/systemone}
 * Authorization: Bearer &lt;apiKey&gt;
 * Content-Type: application/json
 * {
 *   "model": "jev-latest",
 *   "state": { ...object properties... },
 *   "questions": {
 *     "rating-consistency": {
 *       "type": "noul",
 *       "instructions": "Is the sentiment of the review consistent with the numeric rating?"
 *     }
 *   }
 * }
 *
 * 200 OK
 * { "answers": { "rating-consistency": { "type": "noul", "noul": 0.96 } } }
 * </pre>
 *
 * <p>Timeouts are enforced at this adapter level. The API key is only used for
 * the Authorization header: it is never serialized into the request body and
 * never logged. The semantic state itself is never logged either.
 */
public final class TypeSafeJevDecisionModel implements DecisionModel {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI decisionsUri;
    private final String apiKey;
    private final String model;
    private final Duration requestTimeout;

    TypeSafeJevDecisionModel(String baseUrl, String apiKey, String model,
                             Duration connectTimeout, Duration requestTimeout, String decisionsPath) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        if (apiKey == null || apiKey.isBlank()) {
            throw new SemValProviderException("TypeSafe provider requires a non-blank API key");
        }
        this.apiKey = apiKey;
        this.model = model;
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.decisionsUri = URI.create(base + Objects.requireNonNull(decisionsPath, "decisionsPath must not be null"));
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Objects.requireNonNull(connectTimeout, "connectTimeout must not be null"))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public DecisionResponse decide(DecisionRequest request) {
        String body = serialize(request);
        HttpRequest httpRequest = HttpRequest.newBuilder(decisionsUri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new SemValProviderException("TypeSafe decision call failed (I/O)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SemValProviderException("TypeSafe decision call interrupted", e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new SemValProviderException("TypeSafe decision call failed with HTTP " + response.statusCode());
        }
        return deserialize(response.body());
    }

    private String serialize(DecisionRequest request) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", request.model() != null ? request.model() : model);
            root.set("state", objectMapper.valueToTree(request.state()));
            ObjectNode questions = root.putObject("questions");
            request.questions().forEach(q -> {
                ObjectNode question = questions.putObject(q.id());
                question.put("type", "noul");
                question.put("instructions", q.question());
            });
            return objectMapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new SemValProviderException("Cannot serialize decision request", e);
        }
    }

    private DecisionResponse deserialize(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode answers = root.required("answers");
            List<DecisionAnswer> result = new ArrayList<>();
            answers.fields().forEachRemaining(entry -> {
                JsonNode answer = entry.getValue();
                if (!"noul".equals(answer.path("type").asText()) || !answer.has("noul")) {
                    throw new IllegalArgumentException(
                            "Answer '" + entry.getKey() + "' is not a noul answer: " + answer);
                }
                result.add(new DecisionAnswer(entry.getKey(), answer.get("noul").asDouble()));
            });
            return new DecisionResponse(result);
        } catch (IOException | IllegalArgumentException e) {
            throw new SemValProviderException("Cannot parse TypeSafe decision response", e);
        }
    }
}
