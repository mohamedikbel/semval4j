package io.semval4j.core.engine;

import io.semval4j.core.exception.SemValConfigurationException;
import io.semval4j.core.exception.SemValProviderException;
import io.semval4j.core.exception.SemValUncertainException;
import io.semval4j.core.model.DecisionOutcome;
import io.semval4j.core.model.SemanticAnswer;
import io.semval4j.core.model.SemanticDecisionResult;
import io.semval4j.core.model.SemanticQuestion;
import io.semval4j.core.model.SemanticRuleDefinition;
import io.semval4j.core.model.SemanticRuleResult;
import io.semval4j.core.model.SemanticState;
import io.semval4j.core.model.SemanticValidationResult;
import io.semval4j.core.provider.ProviderExceptions;
import io.semval4j.core.provider.SemanticDecisionProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Core semantic validation engine.
 *
 * <p>All rules of one object are batched into a single provider call. Each YES
 * probability is classified into {@link DecisionOutcome#VALID},
 * {@link DecisionOutcome#UNCERTAIN} or {@link DecisionOutcome#INVALID} using the
 * rule thresholds, and the rule's {@code UncertainPolicy} decides whether an
 * uncertain result passes, fails or raises an error.
 */
public final class SemanticValidationEngine {

    private final SemanticDecisionProvider provider;

    public SemanticValidationEngine(SemanticDecisionProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
    }

    /**
     * Validates one object state against all given rules with a single provider call.
     *
     * @param state snapshot of the validated object
     * @param rules semantic rules declared on the object, may be empty
     * @return the per-rule results; never {@code null}
     * @throws SemValConfigurationException for duplicate rule ids
     * @throws SemValProviderException      when the provider fails or answers are missing
     * @throws SemValUncertainException     when a rule is uncertain with policy ERROR
     */
    public SemanticValidationResult validate(SemanticState state, List<SemanticRuleDefinition> rules) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(rules, "rules must not be null");
        if (rules.isEmpty()) {
            return SemanticValidationResult.empty();
        }

        checkUniqueRuleIds(rules);

        List<SemanticQuestion> questions = rules.stream()
                .map(SemanticRuleDefinition::toQuestion)
                .toList();

        SemanticDecisionResult decision = ProviderExceptions.wrap(() -> provider.evaluate(state, questions));
        if (decision == null) {
            throw new SemValProviderException("Decision provider returned null");
        }

        Map<String, SemanticAnswer> answersById = indexAnswers(decision);

        List<SemanticRuleResult> results = new ArrayList<>(rules.size());
        for (SemanticRuleDefinition rule : rules) {
            SemanticAnswer answer = answersById.get(rule.id());
            if (answer == null) {
                throw new SemValProviderException(
                        "Provider returned no answer for rule '" + rule.id() + "'");
            }
            DecisionOutcome outcome = answer.outcome() != null
                    ? answer.outcome()
                    : rule.outcomeFor(answer.probability());
            results.add(new SemanticRuleResult(rule, answer.probability(), outcome, passed(rule, answer.probability(), outcome)));
        }
        return new SemanticValidationResult(results);
    }

    private static void checkUniqueRuleIds(List<SemanticRuleDefinition> rules) {
        Set<String> seen = new HashSet<>();
        for (SemanticRuleDefinition rule : rules) {
            if (!seen.add(rule.id())) {
                throw new SemValConfigurationException("Duplicate semantic rule id: '" + rule.id() + "'");
            }
        }
    }

    private static Map<String, SemanticAnswer> indexAnswers(SemanticDecisionResult decision) {
        Map<String, SemanticAnswer> byId = new HashMap<>();
        for (SemanticAnswer answer : decision.answers()) {
            SemanticAnswer previous = byId.putIfAbsent(answer.id(), answer);
            if (previous != null) {
                throw new SemValProviderException("Provider returned duplicate answer id: '" + answer.id() + "'");
            }
        }
        return byId;
    }

    private static boolean passed(SemanticRuleDefinition rule, double probability, DecisionOutcome outcome) {
        return switch (outcome) {
            case VALID -> true;
            case INVALID -> false;
            case UNCERTAIN -> switch (rule.uncertain()) {
                case FAIL -> false;
                case PASS -> true;
                case ERROR -> throw new SemValUncertainException(rule.id(), probability);
            };
        };
    }
}
