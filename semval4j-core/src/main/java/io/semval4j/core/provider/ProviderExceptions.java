package io.semval4j.core.provider;

import io.semval4j.core.exception.SemValProviderException;

import java.util.function.Supplier;

/**
 * Enforces the provider isolation contract in one place: only SemVal4j-owned
 * exceptions may escape a provider call. Any other exception is wrapped in
 * {@link SemValProviderException}.
 */
public final class ProviderExceptions {

    private ProviderExceptions() {
    }

    /**
     * Runs a provider call, wrapping foreign exceptions.
     *
     * @param call the provider invocation
     * @param <T>  result type
     * @return the call result
     * @throws SemValProviderException when the call fails with any non-SemVal4j exception
     */
    public static <T> T wrap(Supplier<T> call) {
        try {
            return call.get();
        } catch (SemValProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new SemValProviderException("Decision provider call failed", e);
        }
    }
}
