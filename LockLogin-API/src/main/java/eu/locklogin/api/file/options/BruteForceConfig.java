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

import java.util.concurrent.TimeUnit;

/**
 * BruteForce configuration
 */
public final class BruteForceConfig {

    private final int tries;
    private final int time;

    /**
     * Initialize brute force protection configuration
     *
     * @param maxTries  the maximum amount of tries before
     *                  activating brute force on a player
     * @param blockTime the blocking time
     */
    public BruteForceConfig(final int maxTries, final int blockTime) {
        tries = maxTries;
        time = blockTime;
    }

    /**
     * Get the maximum tries the player has before
     * being blocked
     *
     * @return the maximum tries a player has
     * to login incorrectly after being blocked
     */
    public int getMaxTries() {
        return tries;
    }

    /**
     * Get the amount of time the player will be
     * blocked
     *
     * @return the amount of time the player will
     * be blocked ( in seconds )
     */
    public int getBlockTime() {
        return (int) TimeUnit.MINUTES.toSeconds(time);
    }
}
