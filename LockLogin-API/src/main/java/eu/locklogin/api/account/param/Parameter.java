package eu.locklogin.api.account.param;

import org.jetbrains.annotations.NotNull;

/**
 * The parameter of the {@link AccountConstructor}
 *
 * @param <T> the parameter type
 */
public abstract class Parameter<T> {

    /**
     * The local parameter name.
     *
     * This should be used as an unique identifier of
     * the parameter
     *
     * @return the parameter unique local name
     */
    @NotNull
    public abstract String getLocalName();

    /**
     * Get the parameter value
     *
     * @return the parameter value
     */
    @NotNull
    public abstract T getValue();
}
