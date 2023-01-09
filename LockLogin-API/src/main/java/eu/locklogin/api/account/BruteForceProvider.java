package eu.locklogin.api.account;

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

import eu.locklogin.api.module.plugin.javamodule.sender.ModulePlayer;
import ml.karmaconfigs.api.common.string.random.RandomString;

import java.net.InetAddress;

/**
 * LockLogin brute force prevention provider
 */
@SuppressWarnings("unused")
public abstract class BruteForceProvider {

    private final ModulePlayer victim;
    private final InetAddress attacker;

    /**
     * Initialize the brute force provider
     *
     * @param player the player that is trying to be accessed
     * @param ip     the IP that is trying to access the player
     */
    public BruteForceProvider(final ModulePlayer player, final InetAddress ip) {
        victim = player;
        attacker = ip;
    }

    /**
     * Login failed
     */
    public abstract void fail();

    /**
     * Set the account panic status
     *
     * @param status the account panic status
     */
    public abstract void setPanicStatus(final boolean status);

    /**
     * Get if the account is in panic mode
     *
     * @return if the account is in panic mode
     */
    public abstract boolean isInPanic();

    /**
     * Login success
     *
     * @return the new login token
     */
    public abstract String success();

    /**
     * Validate a login token
     *
     * @param token the login token
     * @return if the token is valid
     */
    public abstract boolean validate(final String token);

    /**
     * Get if the IP can access that account
     *
     * @return if the IP can access that account
     */
    public abstract boolean canJoin();

    /**
     * Generate a token
     *
     * @return the login token
     */
    protected final String generate() {
        return new RandomString(RandomString.createBuilder().withSize(32)).create();
    }

    /**
     * Get the account that is being tried to
     * be brute forced
     *
     * @return the victim player object
     */
    public final ModulePlayer getPlayer() {
        return victim;
    }

    /**
     * Get the IP that is trying to
     * access the account
     *
     * @return the IP of the attacker
     */
    public final InetAddress getAddress() {
        return attacker;
    }
}
