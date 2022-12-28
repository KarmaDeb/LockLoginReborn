package eu.locklogin.api.common.web.services;

/**
 * Statistic data
 */
public abstract class Statistic {

    /**
     * The statistic key
     *
     * @return the statistic key name
     */
    public abstract String key();

    /**
     * Send the statistic to a server
     */
    public abstract void send();
}
