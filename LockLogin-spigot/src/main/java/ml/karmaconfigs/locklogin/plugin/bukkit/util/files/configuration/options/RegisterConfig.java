package ml.karmaconfigs.locklogin.plugin.bukkit.util.files.configuration.options;

public class RegisterConfig {

    private final boolean blind;
    private final boolean nausea;
    private final int timeout;
    private final int max;
    private final int interval;

    /**
     * Initialize the register configuration
     *
     * @param blindEffect apply blind effects
     * @param nauseaEffect apply nausea effects
     * @param registerTimeOut register time out
     * @param maxAccounts max accounts that can register
     *                    per IP
     * @param messageInterval the register message interval
     */
    public RegisterConfig(final boolean blindEffect, final boolean nauseaEffect, final int registerTimeOut, final int maxAccounts, final int messageInterval) {
        blind = blindEffect;
        nausea = nauseaEffect;
        timeout = registerTimeOut;
        max = maxAccounts;
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
     * Get the maximum accounts allowed to register
     * per ip
     *
     * @return the maximum amount of accounts
     * that can register per IP
     */
    public final int maxAccounts() {
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
