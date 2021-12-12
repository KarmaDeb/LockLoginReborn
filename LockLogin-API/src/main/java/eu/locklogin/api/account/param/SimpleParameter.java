package eu.locklogin.api.account.param;

import org.jetbrains.annotations.NotNull;

/**
 * Simple implementation of {@link Parameter}
 *
 * @param <T> the parameter type
 */
public final class SimpleParameter<T> extends Parameter<T> {

    private final String name;
    private final T value;

    /**
     * Initialize the parameter builder
     *
     * @param n the parameter name
     * @param v the parameter value
     */
    public SimpleParameter(final String n, final T v) {
        name = n;
        value = v;
    }

    /**
     * The local parameter name.
     * <p>
     * This should be used as an unique identifier of
     * the parameter
     *
     * @return the parameter unique local name
     */
    @Override
    public @NotNull String getLocalName() {
        return name;
    }

    /**
     * Get the parameter value
     *
     * @return the parameter value
     */
    @Override
    public @NotNull T getValue() {
        return value;
    }
}
