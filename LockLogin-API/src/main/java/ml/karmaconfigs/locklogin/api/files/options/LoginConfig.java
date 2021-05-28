package ml.karmaconfigs.locklogin.api.files.options;

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
 * Login configuration
 */
public final class LoginConfig {

    private final boolean boss;
    private final boolean blind;
    private final boolean nausea;
    private final int timeout;
    private final int max;
    private final int interval;

    /**
     * Initialize the login configuration
     *
     * @param bossBar         enable boss bar
     * @param blindEffect     apply blind effects
     * @param nauseaEffect    apply nausea effects
     * @param loginTimeOut    login time out
     * @param maxTries        maximum login tries
     * @param messageInterval the login message interval
     */
    public LoginConfig(final boolean bossBar, final boolean blindEffect, final boolean nauseaEffect, final int loginTimeOut, final int maxTries, final int messageInterval) {
        boss = bossBar;
        blind = blindEffect;
        nausea = nauseaEffect;
        timeout = loginTimeOut;
        max = maxTries;
        interval = messageInterval;
    }

    /**
     * Get if the player should see a boss bar
     *
     * @return if the player should see a boss bar
     */
    public final boolean hasBossBar() {
        return boss;
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
