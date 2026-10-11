package io.semval4j.examples.review;

import io.semval4j.core.model.SemanticAnswer;
import io.semval4j.core.model.SemanticDecisionResult;
import io.semval4j.core.model.SemanticQuestion;
import io.semval4j.core.model.SemanticState;
import io.semval4j.core.provider.SemanticDecisionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end proof: {@code @Valid} triggers semantic validation through the
 * starter, with a deterministic offline provider - no API key, no network.
 */
@SpringBootTest(properties = {
        "semval4j.enabled=true",
        "semval4j.provider=typesafe",
        "semval4j.api-key=unused-in-tests"
})
@AutoConfigureMockMvc
class ProductReviewApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CountingTestProvider provider;

    @BeforeEach
    void resetCounter() {
        provider.invocations().set(0);
    }

    @Test
    void oneStarReviewWithPositiveTextIsRejected() throws Exception {
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "product": "Samsung Galaxy S26",
                                  "rating": 1,
                                  "comment": "Excellent phone, fast, great screen, I recommend it!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").value(org.hamcrest.Matchers.containsString("rating-consistency")));
    }

    @Test
    void fiveStarReviewWithPositiveTextIsAccepted() throws Exception {
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "product": "Samsung Galaxy S26",
                                  "rating": 5,
                                  "comment": "Excellent phone, fast, great screen, I recommend it!"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void structurallyInvalidPayloadIsRejectedAndReportsBothKindsOfErrors() throws Exception {
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "product": "",
                                  "rating": 0,
                                  "comment": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(4))
                .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("rating-consistency"))));

        assertThat(provider.invocations()).hasValue(1);
    }

    @Test
    void twoSemanticRulesResultInExactlyOneProviderCall() throws Exception {
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "product": "Samsung Galaxy S26",
                                  "rating": 5,
                                  "comment": "Excellent phone"
                                }
                                """))
                .andExpect(status().isNoContent());

        assertThat(provider.invocations()).hasValue(1);
    }

    /**
     * Deterministic offline provider reusing the demo scenario
     * ({@link DemoSemanticConfiguration#demoProbability}); counts invocations
     * so tests can prove batching.
     */
    static final class CountingTestProvider implements SemanticDecisionProvider {

        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public SemanticDecisionResult evaluate(SemanticState state, List<SemanticQuestion> questions) {
            invocations.incrementAndGet();
            List<SemanticAnswer> answers = questions.stream()
                    .map(q -> new SemanticAnswer(q.id(), DemoSemanticConfiguration.demoProbability(q.id(), state), null))
                    .toList();
            return new SemanticDecisionResult(answers, "test", "test-1");
        }

        AtomicInteger invocations() {
            return invocations;
        }
    }

    @TestConfiguration
    static class TestProviderConfiguration {

        @Bean
        @Primary
        CountingTestProvider countingTestProvider() {
            return new CountingTestProvider();
        }
    }
}
