package io.semval4j.examples.review;

import io.semval4j.core.model.SemanticAnswer;
import io.semval4j.core.model.SemanticDecisionResult;
import io.semval4j.core.model.SemanticQuestion;
import io.semval4j.core.model.SemanticState;
import io.semval4j.core.provider.SemanticDecisionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Local demo provider, active with {@code --spring.profiles.active=demo}.
 *
 * <p>Simulates a Decision Model without any API key or network access: the
 * starter's back-off picks this bean instead of the TypeSafe provider, which
 * demonstrates exactly how a real provider is swapped in.
 */
@Configuration
@Profile("demo")
public class DemoSemanticConfiguration {

    @Bean
    SemanticDecisionProvider demoProvider() {
        return (state, questions) -> {
            List<SemanticAnswer> answers = questions.stream()
                    .map(q -> new SemanticAnswer(q.id(), demoProbability(q.id(), state), null))
                    .toList();
            return new SemanticDecisionResult(answers, "demo", "demo-1");
        };
    }

    /**
     * Deterministic demo "model": sentiment is deemed consistent for ratings
     * {@code >= 4} (0.97) and inconsistent below (0.04); relevance always passes.
     * Shared with the integration tests so the scenario never drifts.
     */
    public static double demoProbability(String ruleId, SemanticState state) {
        if ("rating-consistency".equals(ruleId)) {
            Object rating = state.values().get("rating");
            return rating instanceof Number n && n.intValue() >= 4 ? 0.97 : 0.04;
        }
        return 0.98;
    }
}
