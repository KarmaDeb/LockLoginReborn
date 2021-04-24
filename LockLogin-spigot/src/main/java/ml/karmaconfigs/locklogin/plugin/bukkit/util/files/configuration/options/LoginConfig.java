package ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.options;

public final class LoginConfig {

    private final boolean blind;
    private final boolean nausea;
    private final int timeout;
    private final int max;
    private final int interval;

    /**
     * Initialize the login configuration
     *
     * @param blindEffect apply blind effects
     * @param nauseaEffect apply nausea effects
     * @param loginTimeOut login time out
     * @param maxTries maximum login tries
     * @param messageInterval the login message interval
     */
    public LoginConfig(final boolean blindEffect, final boolean nauseaEffect, final int loginTimeOut, final int maxTries, final int messageInterval) {
        blind = blindEffect;
        nausea = nauseaEffect;
        timeout = loginTimeOut;
        max = maxTries;
        interval = messageInterval;
    }

    /**
     * Get if the player should receive blind
     * effect
     *
     * @return if the player should receive blind effect
     */
    public final boolean blindEffect() {
        return blind;
    }

    /**
     * Get if the player should receive nausea
     * effect
     *
     * @return if the player should receive
     * nausea effect
     */
    public final boolean nauseaEffect() {
        return nausea;
    }

    /**
     * Get the register time out
     *
     * @return the register time out
     */
    public final int timeOut() {
        return timeout;
    }

    /**
     * Get the maximum tries the player has
     * before being kicked
     *
     * @return the maximum amount of login tries
     * before getting kicked
     */
    public final int maxTries() {
        return max;
    }

    /**
     * Get the login message interval
     *
     * @return the login message interval
     */
    public final int getMessageInterval() {
        return interval;
    }
}
