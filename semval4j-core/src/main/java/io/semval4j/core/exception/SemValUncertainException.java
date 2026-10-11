package io.semval4j.core.exception;

/**
 * Raised when a probability falls in the uncertainty band and the rule uses
 * {@code UncertainPolicy.ERROR}.
 */
public class SemValUncertainException extends SemValException {

    private final String ruleId;
    private final double probability;

    public SemValUncertainException(String ruleId, double probability) {
        super("Semantic rule '" + ruleId + "' is uncertain (probability=" + probability + ")");
        this.ruleId = ruleId;
        this.probability = probability;
    }

    /** @return the identifier of the uncertain rule */
    public String ruleId() {
        return ruleId;
    }

    /** @return the YES probability returned by the provider */
    public double probability() {
        return probability;
    }
}
