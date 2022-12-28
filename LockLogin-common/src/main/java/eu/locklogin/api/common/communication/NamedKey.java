package eu.locklogin.api.common.communication;

/**
 * Named key
 *
 * @param <T> the key type
 */
public abstract class NamedKey<T> {

    /**
     * Get the key
     *
     * @return the key
     */
    public abstract T get();

    /**
     * Get the key name
     *
     * @return the key name
     */
    public abstract String getKey();
}
