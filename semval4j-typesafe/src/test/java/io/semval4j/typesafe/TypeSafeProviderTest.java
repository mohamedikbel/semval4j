package io.semval4j.typesafe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.semval4j.core.exception.SemValProviderException;
import io.semval4j.core.model.SemanticDecisionResult;
import io.semval4j.core.model.SemanticQuestion;
import io.semval4j.core.model.SemanticState;
import io.semval4j.core.provider.SemanticDecisionProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract tests against the real Jev Decisions API wire format:
 * noul primitives keyed by question id, batched in one POST.
 */
class TypeSafeProviderTest {

    private static final SemanticState STATE = new SemanticState(Map.of(
            "product", "Samsung Galaxy S26", "rating", 1, "comment", "great phone"));

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private final AtomicInteger httpCalls = new AtomicInteger();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startServer(int status, String responseBody) throws IOException {
        httpCalls.set(0);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(TypeSafeSemanticProvider.DECISIONS_PATH, exchange -> {
            httpCalls.incrementAndGet();
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void sendsRealJevWireFormatAndMapsNoulAnswers() throws IOException {
        String baseUrl = startServer(200, """
                {"id":"gen-dec-1","model":"typesafe/jev-1.13-20260917","provider":"TypeSafe",
                 "answers":{
                   "rating-consistency":{"type":"noul","noul":0.04},
                   "product-relevance":{"type":"noul","noul":0.98}},
                 "usage":{"cost":0.00002,"input_tokens":476,"output_tokens":70}}
                """);

        SemanticDecisionProvider provider = TypeSafeSemanticProvider.builder()
                .baseUrl(baseUrl)
                .apiKey("test-key")
                .model("typesafe/jev-1.13")
                .build();

        SemanticDecisionResult result = provider.evaluate(STATE, List.of(
                new SemanticQuestion("rating-consistency", "Is sentiment consistent with the rating?"),
                new SemanticQuestion("product-relevance", "Is the review about the product?")));

        assertThat(result.answers()).extracting(a -> a.id() + "=" + a.probability())
                .containsExactlyInAnyOrder("rating-consistency=0.04", "product-relevance=0.98");
        assertThat(result.model()).isEqualTo("typesafe/jev-1.13");

        assertThat(httpCalls).hasValue(1);
        JsonNode sent = objectMapper.readTree(lastBody.get());
        assertThat(sent.get("model").asText()).isEqualTo("typesafe/jev-1.13");
        assertThat(sent.get("state").get("rating").asInt()).isEqualTo(1);
        JsonNode questions = sent.get("questions");
        assertThat(questions.get("rating-consistency").get("type").asText()).isEqualTo("noul");
        assertThat(questions.get("rating-consistency").get("instructions").asText())
                .isEqualTo("Is sentiment consistent with the rating?");
        assertThat(questions.get("product-relevance").get("type").asText()).isEqualTo("noul");
    }

    @Test
    void sendsApiKeyOnlyInAuthorizationHeader() throws IOException {
        String baseUrl = startServer(200, """
                {"answers":{"a":{"type":"noul","noul":0.9}}}
                """);

        TypeSafeSemanticProvider.builder()
                .baseUrl(baseUrl)
                .apiKey("super-secret-key")
                .build()
                .evaluate(STATE, List.of(new SemanticQuestion("a", "q")));

        assertThat(lastAuth.get()).isEqualTo("Bearer super-secret-key");
        assertThat(lastBody.get()).doesNotContain("super-secret-key");
    }

    @Test
    void httpErrorBecomesProviderException() throws IOException {
        String baseUrl = startServer(500, "boom");

        SemanticDecisionProvider provider = TypeSafeSemanticProvider.builder()
                .baseUrl(baseUrl)
                .apiKey("test-key")
                .build();

        assertThatThrownBy(() -> provider.evaluate(STATE, List.of(new SemanticQuestion("a", "q"))))
                .isInstanceOf(SemValProviderException.class)
                .hasMessageContaining("500");
    }

    @Test
    void unexpectedAnswerTypeBecomesProviderException() throws IOException {
        String baseUrl = startServer(200, """
                {"answers":{"a":{"type":"choice","choice":"x","probabilities":{"x":1.0},"confidence":0.9}}}
                """);

        SemanticDecisionProvider provider = TypeSafeSemanticProvider.builder()
                .baseUrl(baseUrl)
                .apiKey("test-key")
                .build();

        assertThatThrownBy(() -> provider.evaluate(STATE, List.of(new SemanticQuestion("a", "q"))))
                .isInstanceOf(SemValProviderException.class);
    }

    @Test
    void requestTimeoutBecomesProviderException() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(TypeSafeSemanticProvider.DECISIONS_PATH, exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        SemanticDecisionProvider provider = TypeSafeSemanticProvider.builder()
                .baseUrl(baseUrl)
                .apiKey("test-key")
                .requestTimeout(Duration.ofMillis(300))
                .build();

        assertThatThrownBy(() -> provider.evaluate(STATE, List.of(new SemanticQuestion("a", "q"))))
                .isInstanceOf(SemValProviderException.class);
    }

    @Test
    void blankApiKeyFailsFast() {
        assertThatThrownBy(() -> TypeSafeSemanticProvider.builder().apiKey(" ").build())
                .isInstanceOf(SemValProviderException.class);
        assertThatThrownBy(() -> TypeSafeSemanticProvider.builder().build())
                .isInstanceOf(SemValProviderException.class);
    }
}
