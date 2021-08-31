package eu.locklogin.api.file.options;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

/**
 * Register configuration
 */
public final class RegisterConfig {

    private final boolean boss;
    private final boolean blind;
    private final boolean nausea;
    private final int timeout;
    private final int max;
    private final int interval;

    /**
     * Initialize the register configuration
     *
     * @param bossBar         enable boss bar
     * @param blindEffect     apply blind effects
     * @param nauseaEffect    apply nausea effects
     * @param registerTimeOut register time out
     * @param maxAccounts     max accounts that can register
     *                        per IP
     * @param messageInterval the register message interval
     */
    public RegisterConfig(final boolean bossBar, final boolean blindEffect, final boolean nauseaEffect, final int registerTimeOut, final int maxAccounts, final int messageInterval) {
        boss = bossBar;
        blind = blindEffect;
        nausea = nauseaEffect;
        timeout = registerTimeOut;
        max = maxAccounts;
        interval = messageInterval;
    }

    /**
     * Get if the player should see a boss bar
     *
     * @return if the player should see a boss bar
     */
    public boolean hasBossBar() {
        return boss;
    }

    /**
     * Get if the player should receive blind
     * effect
     *
     * @return if the player should receive blind effect
     */
    public boolean blindEffect() {
        return blind;
    }

    /**
     * Get if the player should receive nausea
     * effect
     *
     * @return if the player should receive
     * nausea effect
     */
    public boolean nauseaEffect() {
        return nausea;
    }

    /**
     * Get the register time out
     *
     * @return the register time out
     */
    public int timeOut() {
        return timeout;
    }

    /**
     * Get the maximum accounts allowed to register
     * per ip
     *
     * @return the maximum amount of accounts
     * that can register per IP
     */
    public int maxAccounts() {
        return max;
    }

    /**
     * Get the login message interval
     *
     * @return the login message interval
     */
    public int getMessageInterval() {
        return interval;
    }
}
