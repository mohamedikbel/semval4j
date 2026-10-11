package io.semval4j.langchain4j;

/**
 * Minimal Decision Model surface, mirroring the LangChain4j decision-model API
 * family: finite choices with probabilities instead of free text generation.
 *
 * <p>This interface is the seam where concrete Decision Model bindings
 * (Jev/TypeSafe, future Clef or OpenAI Decisions) plug in. It lives in the
 * adapter module so that neither {@code semval4j-core} nor
 * {@code semval4j-jakarta} ever depends on a provider SDK.
 */
public interface DecisionModel {

    /**
     * Evaluates all questions of the request against the shared state in one call.
     *
     * @param request batched decision request
     * @return one answer per requested question
     */
    DecisionResponse decide(DecisionRequest request);
}
